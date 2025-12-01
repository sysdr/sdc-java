const express = require('express');
const axios = require('axios');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = 8085;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Service URLs
const COORDINATOR_URL = process.env.COORDINATOR_URL || 'http://storage-coordinator:8081';
const PROMETHEUS_URL = process.env.PROMETHEUS_URL || 'http://prometheus:9090';
const GATEWAY_URL = process.env.GATEWAY_URL || 'http://api-gateway:8080';

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Proxy endpoint to get distribution metrics
app.get('/api/distribution', async (req, res) => {
  try {
    const response = await axios.get(`${COORDINATOR_URL}/api/coordinator/metrics/distribution`, {
      timeout: 5000
    });
    res.json(response.data);
  } catch (error) {
    console.error('Error fetching distribution metrics:', error.message);
    res.status(500).json({ error: 'Failed to fetch distribution metrics' });
  }
});

// Proxy endpoint to get active nodes
app.get('/api/nodes', async (req, res) => {
  try {
    const response = await axios.get(`${COORDINATOR_URL}/api/coordinator/nodes`, {
      timeout: 5000
    });
    res.json(response.data);
  } catch (error) {
    console.error('Error fetching nodes:', error.message);
    res.status(500).json({ error: 'Failed to fetch nodes' });
  }
});

// Proxy endpoint to get node info
app.get('/api/nodes/info', async (req, res) => {
  try {
    const response = await axios.get(`${COORDINATOR_URL}/api/coordinator/nodes/info`, {
      timeout: 5000
    });
    res.json(response.data);
  } catch (error) {
    console.error('Error fetching node info:', error.message);
    res.status(500).json({ error: 'Failed to fetch node info' });
  }
});

// Proxy endpoint to query Prometheus
app.get('/api/prometheus/query', async (req, res) => {
  try {
    const query = req.query.query;
    if (!query) {
      return res.status(400).json({ error: 'Query parameter is required' });
    }
    
    const response = await axios.get(`${PROMETHEUS_URL}/api/v1/query`, {
      params: { query },
      timeout: 10000
    });
    res.json(response.data);
  } catch (error) {
    console.error('Error querying Prometheus:', error.message);
    res.status(500).json({ error: 'Failed to query Prometheus', details: error.message });
  }
});

// Proxy endpoint to query Prometheus range
app.get('/api/prometheus/query_range', async (req, res) => {
    try {
        const query = req.query.query;
        const start = req.query.start || Math.floor(Date.now() / 1000) - 3600; // Default: last hour
        const end = req.query.end || Math.floor(Date.now() / 1000);
        const step = req.query.step || '15s';
        
        if (!query) {
            return res.status(400).json({ error: 'Query parameter is required' });
        }
        
        const response = await axios.get(`${PROMETHEUS_URL}/api/v1/query_range`, {
            params: { query, start, end, step },
            timeout: 10000
        });
        res.json(response.data);
    } catch (error) {
        console.error('Error querying Prometheus range:', error.message);
        res.status(500).json({ error: 'Failed to query Prometheus range', details: error.message });
    }
});

// Proxy endpoint to check gateway health
app.get('/api/gateway/health', async (req, res) => {
    try {
        const response = await axios.get(`${GATEWAY_URL}/api/logs/health`, {
            timeout: 5000
        });
        res.json({ status: 'ok', message: response.data });
    } catch (error) {
        console.error('Error checking gateway health:', error.message);
        res.status(500).json({ status: 'error', message: 'Gateway not available' });
    }
});

// Proxy endpoint to check coordinator health
app.get('/api/coordinator/health', async (req, res) => {
    try {
        const response = await axios.get(`${COORDINATOR_URL}/actuator/health`, {
            timeout: 5000
        });
        res.json({ status: 'ok', data: response.data });
    } catch (error) {
        console.error('Error checking coordinator health:', error.message);
        res.status(500).json({ status: 'error', message: 'Coordinator not available' });
    }
});

// Serve main dashboard page
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`📊 Dashboard server running on port ${PORT}`);
  console.log(`   Coordinator: ${COORDINATOR_URL}`);
  console.log(`   Prometheus: ${PROMETHEUS_URL}`);
  console.log(`   Gateway: ${GATEWAY_URL}`);
});

