const express = require('express');
const axios = require('axios');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = 8888;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Service URLs
const SERVICES = {
  coordinator: process.env.COORDINATOR_URL || 'http://cluster-coordinator:8080',
  writeGateway: process.env.WRITE_GATEWAY_URL || 'http://write-gateway:9090',
  readGateway: process.env.READ_GATEWAY_URL || 'http://read-gateway:9091',
  prometheus: process.env.PROMETHEUS_URL || 'http://prometheus:9090',
  node1: process.env.NODE1_URL || 'http://storage-node-1:8081',
  node2: process.env.NODE2_URL || 'http://storage-node-2:8082',
  node3: process.env.NODE3_URL || 'http://storage-node-3:8083'
};

// Helper function to make requests with error handling
async function proxyRequest(url, options = {}) {
  try {
    const response = await axios({
      url,
      method: options.method || 'GET',
      data: options.data,
      headers: options.headers || {},
      timeout: 5000
    });
    return { success: true, data: response.data };
  } catch (error) {
    return {
      success: false,
      error: error.message,
      status: error.response?.status || 500
    };
  }
}

// API Routes

// Get cluster topology
app.get('/api/topology', async (req, res) => {
  const result = await proxyRequest(`${SERVICES.coordinator}/api/coordinator/topology`);
  res.json(result);
});

// Get nodes for a key
app.get('/api/nodes/:key', async (req, res) => {
  const { key } = req.params;
  const count = req.query.count || 3;
  const result = await proxyRequest(
    `${SERVICES.coordinator}/api/coordinator/nodes/${key}?count=${count}`
  );
  res.json(result);
});

// Write operation
app.post('/api/write', async (req, res) => {
  const result = await proxyRequest(`${SERVICES.writeGateway}/api/write`, {
    method: 'POST',
    data: req.body,
    headers: { 'Content-Type': 'application/json' }
  });
  res.json(result);
});

// Read operation
app.get('/api/read/:key', async (req, res) => {
  const { key } = req.params;
  const result = await proxyRequest(`${SERVICES.readGateway}/api/read/${key}`);
  res.json(result);
});

// Get node health status
app.get('/api/health', async (req, res) => {
  const healthChecks = await Promise.all([
    proxyRequest(`${SERVICES.coordinator}/actuator/health`),
    proxyRequest(`${SERVICES.node1}/actuator/health`),
    proxyRequest(`${SERVICES.node2}/actuator/health`),
    proxyRequest(`${SERVICES.node3}/actuator/health`),
    proxyRequest(`${SERVICES.writeGateway}/actuator/health`),
    proxyRequest(`${SERVICES.readGateway}/actuator/health`)
  ]);

  res.json({
    coordinator: healthChecks[0],
    node1: healthChecks[1],
    node2: healthChecks[2],
    node3: healthChecks[3],
    writeGateway: healthChecks[4],
    readGateway: healthChecks[5]
  });
});

// Prometheus query endpoint
app.get('/api/prometheus/query', async (req, res) => {
  const { query } = req.query;
  if (!query) {
    return res.status(400).json({ success: false, error: 'Query parameter required' });
  }

  const result = await proxyRequest(
    `${SERVICES.prometheus}/api/v1/query?query=${encodeURIComponent(query)}`
  );
  res.json(result);
});

// Prometheus query range endpoint (for graphs)
app.get('/api/prometheus/query_range', async (req, res) => {
  const { query, start, end, step } = req.query;
  if (!query) {
    return res.status(400).json({ success: false, error: 'Query parameter required' });
  }

  const params = new URLSearchParams({
    query,
    start: start || (Date.now() / 1000 - 3600).toString(), // Default: last hour
    end: end || (Date.now() / 1000).toString(),
    step: step || '15s'
  });

  const result = await proxyRequest(
    `${SERVICES.prometheus}/api/v1/query_range?${params.toString()}`
  );
  res.json(result);
});

// Get all metrics
app.get('/api/prometheus/metrics', async (req, res) => {
  const result = await proxyRequest(`${SERVICES.prometheus}/api/v1/label/__name__/values`);
  res.json(result);
});

// Get metrics metadata
app.get('/api/prometheus/metadata', async (req, res) => {
  const result = await proxyRequest(`${SERVICES.prometheus}/api/v1/metadata`);
  res.json(result);
});

// Serve dashboard
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Dashboard server running on port ${PORT}`);
  console.log(`Access dashboard at http://localhost:${PORT}`);
});

