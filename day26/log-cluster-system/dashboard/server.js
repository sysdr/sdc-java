const express = require('express');
const axios = require('axios');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = 3001;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// Service URLs
const SERVICES = {
  coordinator: process.env.COORDINATOR_URL || 'http://cluster-coordinator:8081',
  producer: process.env.PRODUCER_URL || 'http://log-producer:8082',
  consumer: process.env.CONSUMER_URL || 'http://log-consumer:8083',
  gateway: process.env.GATEWAY_URL || 'http://api-gateway:8080',
  prometheus: process.env.PROMETHEUS_URL || 'http://prometheus:9090'
};

// Helper function to make requests with error handling
async function makeRequest(url, options = {}) {
  try {
    const response = await axios({
      url,
      method: options.method || 'GET',
      data: options.data,
      timeout: 10000,
      ...options
    });
    return { success: true, data: response.data, status: response.status };
  } catch (error) {
    return {
      success: false,
      error: error.message,
      status: error.response?.status || 500,
      data: error.response?.data || null
    };
  }
}

// Cluster Operations
app.get('/api/cluster/status', async (req, res) => {
  const result = await makeRequest(`${SERVICES.coordinator}/cluster/status`);
  res.json(result);
});

app.get('/api/cluster/membership', async (req, res) => {
  const result = await makeRequest(`${SERVICES.coordinator}/cluster/membership`);
  res.json(result);
});

app.post('/api/cluster/gossip', async (req, res) => {
  const result = await makeRequest(`${SERVICES.coordinator}/cluster/gossip`, {
    method: 'POST',
    data: req.body
  });
  res.json(result);
});

app.post('/api/cluster/anti-entropy', async (req, res) => {
  const result = await makeRequest(`${SERVICES.coordinator}/cluster/anti-entropy`, {
    method: 'POST',
    data: req.body
  });
  res.json(result);
});

// Health Checks
// IMPORTANT: More specific routes must come before parameterized routes
app.get('/api/health/all', async (req, res) => {
  const services = ['coordinator', 'producer', 'consumer', 'gateway'];
  const results = {};
  
  for (const service of services) {
    let url;
    switch(service) {
      case 'coordinator':
        url = `${SERVICES.coordinator}/actuator/health`;
        break;
      case 'producer':
        url = `${SERVICES.producer}/actuator/health`;
        break;
      case 'consumer':
        url = `${SERVICES.consumer}/actuator/health`;
        break;
      case 'gateway':
        url = `${SERVICES.gateway}/actuator/health`;
        break;
    }
    const result = await makeRequest(url);
    results[service] = result;
  }
  
  res.json({ success: true, data: results });
});

app.get('/api/health/:service', async (req, res) => {
  const service = req.params.service;
  let url;
  
  switch(service) {
    case 'coordinator':
      url = `${SERVICES.coordinator}/actuator/health`;
      break;
    case 'producer':
      url = `${SERVICES.producer}/actuator/health`;
      break;
    case 'consumer':
      url = `${SERVICES.consumer}/actuator/health`;
      break;
    case 'gateway':
      url = `${SERVICES.gateway}/actuator/health`;
      break;
    default:
      return res.status(400).json({ success: false, error: 'Invalid service name' });
  }
  
  const result = await makeRequest(url);
  res.json(result);
});

// Metrics
app.get('/api/metrics/:service', async (req, res) => {
  const service = req.params.service;
  let url;
  
  switch(service) {
    case 'coordinator':
      url = `${SERVICES.coordinator}/actuator/prometheus`;
      break;
    case 'producer':
      url = `${SERVICES.producer}/actuator/prometheus`;
      break;
    case 'consumer':
      url = `${SERVICES.consumer}/actuator/prometheus`;
      break;
    case 'gateway':
      url = `${SERVICES.gateway}/actuator/prometheus`;
      break;
    default:
      return res.status(400).json({ success: false, error: 'Invalid service name' });
  }
  
  const result = await makeRequest(url);
  res.json(result);
});

// Prometheus Integration
app.get('/api/prometheus/query', async (req, res) => {
  const query = req.query.q;
  if (!query) {
    return res.status(400).json({ success: false, error: 'Query parameter "q" is required' });
  }
  
  const url = `${SERVICES.prometheus}/api/v1/query?query=${encodeURIComponent(query)}`;
  const result = await makeRequest(url);
  res.json(result);
});

app.get('/api/prometheus/query-range', async (req, res) => {
  const query = req.query.q || req.query.query;
  const { start, end, step } = req.query;
  if (!query) {
    return res.status(400).json({ success: false, error: 'Query parameter "q" is required' });
  }
  
  let url = `${SERVICES.prometheus}/api/v1/query_range?query=${encodeURIComponent(query)}`;
  if (start) url += `&start=${start}`;
  if (end) url += `&end=${end}`;
  if (step) url += `&step=${step}`;
  
  const result = await makeRequest(url);
  res.json(result);
});

app.get('/api/prometheus/targets', async (req, res) => {
  const url = `${SERVICES.prometheus}/api/v1/targets`;
  const result = await makeRequest(url);
  res.json(result);
});

// Serve dashboard
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`📊 Dashboard server running on port ${PORT}`);
  console.log(`🌐 Access at http://localhost:${PORT}`);
});

