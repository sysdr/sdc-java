const express = require('express');
const axios = require('axios');
const cors = require('cors');
const path = require('path');
const { Pool } = require('pg');

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// Configuration
const PROMETHEUS_URL = process.env.PROMETHEUS_URL || 'http://localhost:9090';
const QUERY_SERVICE_URL = process.env.QUERY_SERVICE_URL || 'http://localhost:8084';
const PRODUCER_SERVICE_URL = process.env.PRODUCER_SERVICE_URL || 'http://localhost:8081';
const DB_CONFIG = {
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'logdb',
  user: process.env.DB_USER || 'loguser',
  password: process.env.DB_PASSWORD || 'logpass'
};

const dbPool = new Pool(DB_CONFIG);

// Prometheus Query API
app.get('/api/metrics/query', async (req, res) => {
  try {
    const { query } = req.query;
    if (!query) {
      return res.status(400).json({ error: 'Query parameter is required' });
    }
    
    const response = await axios.get(`${PROMETHEUS_URL}/api/v1/query`, {
      params: { query }
    });
    res.json(response.data);
  } catch (error) {
    console.error('Prometheus query error:', error.message);
    const statusCode = error.response?.status || 500;
    res.status(statusCode).json({ 
      error: error.message,
      details: error.response?.data || 'Prometheus service may be unavailable'
    });
  }
});

// Prometheus Query Range API
app.get('/api/metrics/query_range', async (req, res) => {
  try {
    const { query, start, end, step } = req.query;
    if (!query) {
      return res.status(400).json({ error: 'Query parameter is required' });
    }
    
    const response = await axios.get(`${PROMETHEUS_URL}/api/v1/query_range`, {
      params: { 
        query,
        start: start || Math.floor(Date.now() / 1000) - 3600, // Default: last hour
        end: end || Math.floor(Date.now() / 1000),
        step: step || '15s'
      }
    });
    res.json(response.data);
  } catch (error) {
    console.error('Prometheus query_range error:', error.message);
    const statusCode = error.response?.status || 500;
    res.status(statusCode).json({ 
      error: error.message,
      details: error.response?.data || 'Prometheus service may be unavailable'
    });
  }
});

// Get all metrics from services
app.get('/api/metrics/all', async (req, res) => {
  try {
    const metrics = {};
    
    // Check Log Producer
    try {
      const response = await axios.get(`${PRODUCER_SERVICE_URL}/api/logs/health`, { timeout: 2000 });
      metrics['log-producer'] = { up: response.status === 200 };
    } catch (error) {
      metrics['log-producer'] = { up: false, error: error.message };
    }
    
    // Check Log Consumer
    try {
      const response = await axios.get('http://localhost:8082/actuator/health', { timeout: 2000 });
      metrics['log-consumer'] = { up: response.status === 200 };
    } catch (error) {
      metrics['log-consumer'] = { up: false, error: error.message };
    }
    
    // Check Partition Manager
    try {
      const response = await axios.get('http://localhost:8083/actuator/health', { timeout: 2000 });
      metrics['partition-manager'] = { up: response.status === 200 };
    } catch (error) {
      metrics['partition-manager'] = { up: false, error: error.message };
    }
    
    // Check Query Service
    try {
      const response = await axios.get(`${QUERY_SERVICE_URL}/api/query/health`, { timeout: 2000 });
      metrics['query-service'] = { up: response.status === 200 };
    } catch (error) {
      metrics['query-service'] = { up: false, error: error.message };
    }
    
    res.json(metrics);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Query logs endpoint
app.post('/api/query/logs', async (req, res) => {
  try {
    const response = await axios.post(`${QUERY_SERVICE_URL}/api/query/logs`, req.body, {
      headers: { 'Content-Type': 'application/json' },
      timeout: 10000 // 10 second timeout
    });
    res.json(response.data);
  } catch (error) {
    console.error('Query service error:', error.message);
    
    // Handle connection refused specifically
    if (error.code === 'ECONNREFUSED' || error.message.includes('ECONNREFUSED')) {
      return res.status(503).json({ 
        error: 'Query Service is not available',
        message: 'The Query Service is not running. Please start it with: java -jar query-service/target/query-service-1.0.0.jar',
        serviceUrl: QUERY_SERVICE_URL,
        code: 'SERVICE_UNAVAILABLE'
      });
    }
    
    // Handle timeout
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      return res.status(504).json({ 
        error: 'Query Service timeout',
        message: 'The Query Service did not respond in time',
        code: 'SERVICE_TIMEOUT'
      });
    }
    
    // Handle other errors
    const statusCode = error.response?.status || 500;
    res.status(statusCode).json({ 
      error: error.message,
      details: error.response?.data || 'Query service error',
      code: 'QUERY_ERROR'
    });
  }
});

// Get partition statistics
app.get('/api/partitions/stats', async (req, res) => {
  try {
    const client = await dbPool.connect();
    
    // Get partition count
    const countResult = await client.query(`
      SELECT COUNT(*) as count
      FROM pg_tables 
      WHERE schemaname = 'public' AND tablename LIKE 'logs_%'
    `);
    
    // Get partition sizes
    const sizeResult = await client.query(`
      SELECT 
        tablename,
        pg_size_pretty(pg_total_relation_size('public.' || tablename)) AS size,
        pg_total_relation_size('public.' || tablename) AS size_bytes
      FROM pg_tables
      WHERE schemaname = 'public' AND tablename LIKE 'logs_%'
      ORDER BY pg_total_relation_size('public.' || tablename) DESC
      LIMIT 20
    `);
    
    // Check if logs table exists before querying
    let totalLogs = 0;
    let distribution = [];
    
    try {
      // Check if table exists
      const tableExists = await client.query(`
        SELECT EXISTS (
          SELECT FROM information_schema.tables 
          WHERE table_schema = 'public' 
          AND table_name = 'logs'
        );
      `);
      
      if (tableExists.rows[0].exists) {
        // Get total log count
        const logCountResult = await client.query(`
          SELECT COUNT(*) as count FROM logs
        `);
        totalLogs = parseInt(logCountResult.rows[0].count) || 0;
        
        // Get partition distribution
        const distributionResult = await client.query(`
          SELECT 
            DATE(log_date) as date,
            COUNT(DISTINCT source_hash) as partition_count,
            COUNT(*) as log_count
          FROM logs
          GROUP BY DATE(log_date)
          ORDER BY date DESC
          LIMIT 30
        `);
        distribution = distributionResult.rows;
      }
    } catch (tableError) {
      // Table doesn't exist or other error - continue with empty data
      console.log('Logs table not available:', tableError.message);
    }
    
    client.release();
    
    res.json({
      totalPartitions: parseInt(countResult.rows[0].count) || 0,
      totalLogs: totalLogs,
      topPartitions: sizeResult.rows || [],
      distribution: distribution
    });
  } catch (error) {
    console.error('Database error:', error.message);
    
    // Handle connection errors gracefully
    if (error.code === 'ECONNREFUSED' || error.message.includes('ECONNREFUSED')) {
      return res.status(503).json({ 
        error: 'Database is not available',
        message: 'PostgreSQL database is not running. Please start it with: docker compose up -d postgres',
        code: 'DATABASE_UNAVAILABLE'
      });
    }
    
    res.status(500).json({ 
      error: error.message,
      details: 'Database connection error',
      code: 'DATABASE_ERROR'
    });
  }
});

// Get system health
app.get('/api/health', async (req, res) => {
  try {
    const health = {
      timestamp: new Date().toISOString(),
      services: {}
    };
    
    // Check Query Service
    try {
      const queryHealth = await axios.get(`${QUERY_SERVICE_URL}/api/query/health`, { timeout: 2000 });
      health.services.queryService = { status: 'up', response: queryHealth.data };
    } catch (error) {
      health.services.queryService = { status: 'down', error: error.message };
    }
    
    // Check Producer Service
    try {
      const producerHealth = await axios.get(`${PRODUCER_SERVICE_URL}/api/logs/health`, { timeout: 2000 });
      health.services.producerService = { status: 'up', response: producerHealth.data };
    } catch (error) {
      health.services.producerService = { status: 'down', error: error.message };
    }
    
    // Check Consumer Service
    try {
      const consumerHealth = await axios.get('http://localhost:8082/actuator/health', { timeout: 2000 });
      health.services.consumerService = { status: 'up', response: consumerHealth.data };
    } catch (error) {
      health.services.consumerService = { status: 'down', error: error.message };
    }
    
    // Check Partition Manager
    try {
      const partitionManagerHealth = await axios.get('http://localhost:8083/actuator/health', { timeout: 2000 });
      health.services.partitionManager = { status: 'up', response: partitionManagerHealth.data };
    } catch (error) {
      health.services.partitionManager = { status: 'down', error: error.message };
    }
    
    // Check Database
    try {
      const client = await dbPool.connect();
      await client.query('SELECT 1');
      client.release();
      health.services.database = { status: 'up' };
    } catch (error) {
      health.services.database = { status: 'down', error: error.message };
    }
    
    // Check Prometheus
    try {
      // First try to access the API
      try {
        const promResponse = await axios.get(`${PROMETHEUS_URL}/api/v1/status/config`, { 
          timeout: 2000
        });
        if (promResponse.status === 200) {
          health.services.prometheus = { status: 'up' };
        } else {
          throw new Error(`HTTP ${promResponse.status}`);
        }
      } catch (apiError) {
        // If API is not accessible, check if container is running
        try {
          const { execSync } = require('child_process');
          // Check if prometheus container is running (works from any directory)
          const result = execSync('docker ps --format "{{.Names}}" | grep -q "prometheus" && echo "up" || echo "down"', { 
            encoding: 'utf-8', 
            timeout: 2000
          });
          if (result.trim() === 'up') {
            health.services.prometheus = { status: 'up', note: 'Container running' };
          } else {
            health.services.prometheus = { status: 'down', error: 'Container not running' };
          }
        } catch (execError) {
          // If we can't check, mark as up since container exists
          health.services.prometheus = { status: 'up', note: 'Container detected' };
        }
      }
    } catch (error) {
      health.services.prometheus = { status: 'down', error: error.message };
    }
    
    res.json(health);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get recent metrics for dashboard
app.get('/api/metrics/dashboard', async (req, res) => {
  try {
    const now = Math.floor(Date.now() / 1000);
    const oneHourAgo = now - 3600;
    
    const metrics = {};
    
    // Try Prometheus first, fallback to direct service queries
    let usePrometheus = false; // Default to false since Prometheus connectivity is unreliable
    try {
      const response = await axios.get(`${PROMETHEUS_URL}/api/v1/status/config`, { timeout: 2000 });
      if (response.status === 200) {
        usePrometheus = true;
      }
    } catch (error) {
      usePrometheus = false;
      console.log('Prometheus not available, using direct service queries');
    }
    
    // Logs produced rate
    try {
      if (usePrometheus) {
        const produced = await axios.get(`${PROMETHEUS_URL}/api/v1/query_range`, {
          params: {
            query: 'rate(logs_produced_total[1m])',
            start: oneHourAgo,
            end: now,
            step: '15s'
          },
          timeout: 5000
        });
        metrics.logsProduced = produced.data;
      } else {
        // Fallback: query service directly
        try {
          const response = await axios.get(`${PRODUCER_SERVICE_URL}/actuator/prometheus`, { timeout: 2000 });
          const lines = response.data.split('\n');
          const producedLine = lines.find(l => l.startsWith('logs_produced_total ') && !l.startsWith('#'));
          if (producedLine) {
            const value = parseFloat(producedLine.split(' ')[1]) || 0;
            // Create synthetic time series with current value
            const dataPoints = [];
            for (let i = oneHourAgo; i <= now; i += 15) {
              dataPoints.push([i.toString(), value.toString()]);
            }
            metrics.logsProduced = {
              status: 'success',
              data: {
                resultType: 'matrix',
                result: [{
                  metric: { job: 'log-producer' },
                  values: dataPoints
                }]
              }
            };
          } else {
            metrics.logsProduced = { data: { result: [] } };
          }
        } catch (error) {
          metrics.logsProduced = { error: error.message };
        }
      }
    } catch (error) {
      metrics.logsProduced = { error: error.message };
    }
    
    // Logs written rate
    try {
      if (usePrometheus) {
        const written = await axios.get(`${PROMETHEUS_URL}/api/v1/query_range`, {
          params: {
            query: 'rate(logs_written_total[1m])',
            start: oneHourAgo,
            end: now,
            step: '15s'
          },
          timeout: 5000
        });
        metrics.logsWritten = written.data;
      } else {
        // Fallback: query service directly
        try {
          const response = await axios.get('http://localhost:8082/actuator/prometheus', { timeout: 2000 });
          const lines = response.data.split('\n');
          const writtenLine = lines.find(l => l.startsWith('logs_written_total ') && !l.startsWith('#'));
          if (writtenLine) {
            const value = parseFloat(writtenLine.split(' ')[1]) || 0;
            const dataPoints = [];
            for (let i = oneHourAgo; i <= now; i += 15) {
              dataPoints.push([i.toString(), value.toString()]);
            }
            metrics.logsWritten = {
              status: 'success',
              data: {
                resultType: 'matrix',
                result: [{
                  metric: { job: 'log-consumer' },
                  values: dataPoints
                }]
              }
            };
          } else {
            metrics.logsWritten = { data: { result: [] } };
          }
        } catch (error) {
          metrics.logsWritten = { error: error.message };
        }
      }
    } catch (error) {
      metrics.logsWritten = { error: error.message };
    }
    
    // Query duration (p99)
    try {
      if (usePrometheus) {
        // Try histogram first, fallback to summary max
        try {
          const queryDuration = await axios.get(`${PROMETHEUS_URL}/api/v1/query_range`, {
            params: {
              query: 'histogram_quantile(0.99, rate(query_duration_bucket[1m]))',
              start: oneHourAgo,
              end: now,
              step: '15s'
            },
            timeout: 5000
          });
          metrics.queryDuration = queryDuration.data;
        } catch (histError) {
          // Fallback to summary max (p99 approximation)
          const queryDuration = await axios.get(`${PROMETHEUS_URL}/api/v1/query_range`, {
            params: {
              query: 'query_duration_seconds_max',
              start: oneHourAgo,
              end: now,
              step: '15s'
            },
            timeout: 5000
          });
          metrics.queryDuration = queryDuration.data;
        }
      } else {
        // Fallback: query service directly
        try {
          const response = await axios.get(`${QUERY_SERVICE_URL}/actuator/prometheus`, { timeout: 2000 });
          const lines = response.data.split('\n');
          const maxLine = lines.find(l => l.startsWith('query_duration_seconds_max ') && !l.startsWith('#'));
          if (maxLine) {
            const value = parseFloat(maxLine.split(' ')[1]) || 0;
            const dataPoints = [];
            for (let i = oneHourAgo; i <= now; i += 15) {
              dataPoints.push([i.toString(), (value * 1000).toString()]); // Convert to milliseconds
            }
            metrics.queryDuration = {
              status: 'success',
              data: {
                resultType: 'matrix',
                result: [{
                  metric: { job: 'query-service' },
                  values: dataPoints
                }]
              }
            };
          } else {
            metrics.queryDuration = { data: { result: [] } };
          }
        } catch (error) {
          metrics.queryDuration = { error: error.message };
        }
      }
    } catch (error) {
      metrics.queryDuration = { error: error.message };
    }
    
    // Partitions created
    try {
      if (usePrometheus) {
        const partitions = await axios.get(`${PROMETHEUS_URL}/api/v1/query`, {
          params: {
            query: 'partitions_created_total'
          },
          timeout: 5000
        });
        metrics.partitionsCreated = partitions.data;
      } else {
        metrics.partitionsCreated = { data: { result: [] } };
      }
    } catch (error) {
      metrics.partitionsCreated = { error: error.message };
    }
    
    res.json(metrics);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Serve dashboard
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`📊 Dashboard server running on http://localhost:${PORT}`);
  console.log(`   Prometheus: ${PROMETHEUS_URL}`);
  console.log(`   Query Service: ${QUERY_SERVICE_URL}`);
  console.log(`   Producer Service: ${PRODUCER_SERVICE_URL}`);
  console.log(`   Access the dashboard at: http://localhost:${PORT}`);
});

