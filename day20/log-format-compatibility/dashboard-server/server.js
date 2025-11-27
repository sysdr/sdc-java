const express = require('express');
const axios = require('axios');
const cors = require('cors');
const path = require('path');
const http = require('http');
const WebSocket = require('ws');

const app = express();
const PORT = 8085;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Service endpoints
const SERVICES = {
  syslogAdapter: 'http://localhost:8081',
  journaldAdapter: 'http://localhost:8082',
  formatNormalizer: 'http://localhost:8083',
  apiGateway: 'http://localhost:8080',
  prometheus: 'http://localhost:9090',
  grafana: 'http://localhost:3000'
};

// Health check endpoint
app.get('/api/health', async (req, res) => {
  const health = {};
  
  for (const [name, url] of Object.entries(SERVICES)) {
    try {
      if (name === 'prometheus') {
        await axios.get(`${url}/api/v1/status/config`, { timeout: 2000 });
        health[name] = { status: 'UP', url };
      } else if (name === 'grafana') {
        await axios.get(`${url}/api/health`, { timeout: 2000 });
        health[name] = { status: 'UP', url };
      } else {
        const response = await axios.get(`${url}/actuator/health`, { timeout: 2000 });
        health[name] = { status: response.data.status || 'UP', url };
      }
    } catch (error) {
      health[name] = { status: 'DOWN', url, error: error.message };
    }
  }
  
  res.json(health);
});

// Get service statistics
app.get('/api/stats', async (req, res) => {
  try {
    const response = await axios.get(`${SERVICES.apiGateway}/api/logs/stats`, { timeout: 2000 });
    res.json(response.data);
  } catch (error) {
    // Return default stats when service is unavailable
    res.json({
      total: 0,
      syslog: 0,
      journald: 0,
      normalized: 0,
      error: 'API Gateway not available'
    });
  }
});

// Query logs
app.get('/api/logs/search', async (req, res) => {
  try {
    const params = new URLSearchParams(req.query);
    const response = await axios.get(`${SERVICES.apiGateway}/api/logs/search?${params}`);
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Prometheus metrics proxy
app.get('/api/prometheus/query', async (req, res) => {
  try {
    const { query } = req.query;
    if (!query) {
      return res.status(400).json({ error: 'Query parameter required' });
    }
    
    const response = await axios.get(`${SERVICES.prometheus}/api/v1/query`, {
      params: { query },
      timeout: 10000
    });
    
    // Check if Prometheus returned an error
    if (response.data.status === 'error') {
      return res.status(500).json({ 
        error: response.data.error || 'Prometheus query error',
        details: response.data
      });
    }
    
    res.json(response.data);
  } catch (error) {
    console.error('Prometheus query error:', error.message);
    if (error.response) {
      return res.status(error.response.status || 500).json({ 
        error: error.response.data?.error || error.message,
        details: error.response.data
      });
    } else if (error.request) {
      return res.status(503).json({ 
        error: 'Prometheus is not accessible',
        message: 'Cannot connect to Prometheus. Please ensure Prometheus is running on localhost:9090'
      });
    } else {
      return res.status(500).json({ 
        error: error.message || 'Unknown error occurred'
      });
    }
  }
});

// Prometheus query range for time series
app.get('/api/prometheus/query_range', async (req, res) => {
  try {
    const { query, start, end, step } = req.query;
    if (!query) {
      return res.status(400).json({ error: 'Query parameter required' });
    }
    
    // Convert start and end to numbers if they're strings
    const startTime = start ? (typeof start === 'string' ? parseInt(start) : start) : Math.floor(Date.now() / 1000) - 3600;
    const endTime = end ? (typeof end === 'string' ? parseInt(end) : end) : Math.floor(Date.now() / 1000);
    const stepValue = step || '15s';
    
    const params = {
      query,
      start: startTime,
      end: endTime,
      step: stepValue
    };
    
    console.log(`Prometheus query_range: ${query}, start: ${startTime}, end: ${endTime}, step: ${stepValue}`);
    
    const response = await axios.get(`${SERVICES.prometheus}/api/v1/query_range`, { 
      params,
      timeout: 10000 // 10 second timeout
    });
    
    // Check if Prometheus returned an error
    if (response.data.status === 'error') {
      return res.status(500).json({ 
        error: response.data.error || 'Prometheus query error',
        details: response.data
      });
    }
    
    res.json(response.data);
  } catch (error) {
    console.error('Prometheus query_range error:', error.message);
    if (error.response) {
      // Prometheus returned an error response
      return res.status(error.response.status || 500).json({ 
        error: error.response.data?.error || error.message,
        details: error.response.data
      });
    } else if (error.request) {
      // Request was made but no response received
      return res.status(503).json({ 
        error: 'Prometheus is not accessible',
        message: 'Cannot connect to Prometheus. Please ensure Prometheus is running on localhost:9090'
      });
    } else {
      // Error setting up the request
      return res.status(500).json({ 
        error: error.message || 'Unknown error occurred'
      });
    }
  }
});

// Get all metrics from Prometheus
app.get('/api/prometheus/metrics', async (req, res) => {
  try {
    const response = await axios.get(`${SERVICES.prometheus}/api/v1/label/__name__/values`);
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Metrics aggregation endpoint for Prometheus to scrape
app.get('/metrics', async (req, res) => {
  try {
    const metrics = [];
    
    // Fetch metrics from all services
    const services = [
      { name: 'syslog-adapter', url: SERVICES.syslogAdapter },
      { name: 'journald-adapter', url: SERVICES.journaldAdapter },
      { name: 'format-normalizer', url: SERVICES.formatNormalizer },
      { name: 'api-gateway', url: SERVICES.apiGateway }
    ];
    
    for (const service of services) {
      try {
        const response = await axios.get(`${service.url}/actuator/prometheus`, { timeout: 2000 });
        // Add service label to each metric line
        const serviceMetrics = response.data.split('\n')
          .filter(line => line && !line.startsWith('#'))
          .map(line => {
            if (line.includes('{')) {
              return line.replace('{', `{service="${service.name}",`);
            } else if (line.includes(' ')) {
              const parts = line.split(' ');
              return `${parts[0]}{service="${service.name}"} ${parts[1]}`;
            }
            return line;
          });
        metrics.push(...serviceMetrics);
      } catch (error) {
        console.error(`Error fetching metrics from ${service.name}:`, error.message);
      }
    }
    
    res.set('Content-Type', 'text/plain');
    res.send(metrics.join('\n') + '\n');
  } catch (error) {
    res.status(500).send(`# Error: ${error.message}\n`);
  }
});

// Kafka topics info (via docker exec)
app.get('/api/kafka/topics', async (req, res) => {
  try {
    const { exec } = require('child_process');
    exec('docker ps -qf "name=kafka"', (error, stdout, stderr) => {
      if (error) {
        return res.status(500).json({ error: 'Kafka container not found' });
      }
      const containerId = stdout.trim();
      if (!containerId) {
        return res.status(500).json({ error: 'Kafka container not running' });
      }
      
      exec(`docker exec ${containerId} kafka-topics --list --bootstrap-server localhost:9092`, 
        (error, stdout, stderr) => {
          if (error) {
            return res.status(500).json({ error: error.message });
          }
          const topics = stdout.trim().split('\n').filter(t => t);
          res.json({ topics });
        }
      );
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// System architecture info
app.get('/api/system/info', (req, res) => {
  res.json({
    architecture: {
      services: [
        {
          name: 'Syslog Adapter',
          port: 8081,
          description: 'UDP server on port 514 for traditional syslog, TCP server on port 601 for reliable syslog. Parses RFC 3164 and RFC 5424 formats.',
          topics: ['raw-syslog-logs']
        },
        {
          name: 'Journald Adapter',
          port: 8082,
          description: 'Polls systemd journal via journalctl. Maintains cursor position in Redis.',
          topics: ['raw-journald-logs']
        },
        {
          name: 'Format Normalizer',
          port: 8083,
          description: 'Consumes from raw topics, normalizes to unified schema, validates and enriches events.',
          topics: ['normalized-logs']
        },
        {
          name: 'API Gateway',
          port: 8080,
          description: 'REST API for log queries with in-memory cache of recent logs and statistics endpoints.',
          topics: []
        }
      ],
      infrastructure: [
        { name: 'Kafka', port: 9092, description: 'Message broker for log streaming' },
        { name: 'Redis', port: 6379, description: 'Cursor position storage for journald adapter' },
        { name: 'PostgreSQL', port: 5432, description: 'Database for log storage' },
        { name: 'Prometheus', port: 9090, description: 'Metrics collection and querying' },
        { name: 'Grafana', port: 3000, description: 'Visualization and dashboards' }
      ]
    }
  });
});

// Serve favicon (handle 404 gracefully)
app.get('/favicon.ico', (req, res) => {
  res.status(204).end();
});

// Serve main dashboard
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// WebSocket server for real-time updates
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

wss.on('connection', (ws) => {
  console.log('Client connected');
  
  const interval = setInterval(async () => {
    try {
      // Send health status
      const healthResponse = await axios.get(`http://localhost:${PORT}/api/health`);
      ws.send(JSON.stringify({ type: 'health', data: healthResponse.data }));
      
      // Send stats
      try {
        const statsResponse = await axios.get(`http://localhost:${PORT}/api/stats`);
        ws.send(JSON.stringify({ type: 'stats', data: statsResponse.data }));
      } catch (e) {
        // Stats might fail if services aren't running
      }
    } catch (error) {
      ws.send(JSON.stringify({ type: 'error', data: error.message }));
    }
  }, 5000); // Update every 5 seconds
  
  ws.on('close', () => {
    clearInterval(interval);
    console.log('Client disconnected');
  });
});

server.listen(PORT, () => {
  console.log(`Dashboard server running on http://localhost:${PORT}`);
  console.log(`Monitoring services at:`);
  Object.entries(SERVICES).forEach(([name, url]) => {
    console.log(`  ${name}: ${url}`);
  });
});

