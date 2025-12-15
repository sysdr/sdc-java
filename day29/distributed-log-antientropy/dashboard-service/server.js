const express = require('express');
const axios = require('axios');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 8088;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Service URLs
const SERVICES = {
  apiGateway: process.env.API_GATEWAY_URL || 'http://api-gateway:8080',
  node1: process.env.NODE1_URL || 'http://node1:8081',
  node2: process.env.NODE2_URL || 'http://node2:8082',
  node3: process.env.NODE3_URL || 'http://node3:8083',
  merkleTree: process.env.MERKLE_TREE_URL || 'http://merkle-tree-service:8084',
  coordinator: process.env.COORDINATOR_URL || 'http://anti-entropy-coordinator:8085',
  readRepair: process.env.READ_REPAIR_URL || 'http://read-repair-service:8086',
  hintManager: process.env.HINT_MANAGER_URL || 'http://hint-manager:8087',
  prometheus: process.env.PROMETHEUS_URL || 'http://prometheus:9090',
  grafana: process.env.GRAFANA_URL || 'http://grafana:3000'
};

// Helper function to make requests with error handling
async function makeRequest(url, method = 'GET', data = null) {
  try {
    const config = {
      method,
      url,
      timeout: 5000,
      ...(data && { data })
    };
    const response = await axios(config);
    return { success: true, data: response.data, status: response.status };
  } catch (error) {
    return {
      success: false,
      error: error.message,
      status: error.response?.status || 500
    };
  }
}

// Health check endpoint - aggregates all service health
app.get('/api/health', async (req, res) => {
  const healthChecks = {};
  
  const services = [
    { name: 'API Gateway', url: `${SERVICES.apiGateway}/api/health` },
    { name: 'Node 1', url: `${SERVICES.node1}/api/storage/health` },
    { name: 'Node 2', url: `${SERVICES.node2}/api/storage/health` },
    { name: 'Node 3', url: `${SERVICES.node3}/api/storage/health` },
    { name: 'Merkle Tree Service', url: `${SERVICES.merkleTree}/api/merkle/health` },
    { name: 'Anti-Entropy Coordinator', url: `${SERVICES.coordinator}/api/coordinator/health` },
    { name: 'Read Repair Service', url: `${SERVICES.readRepair}/api/read-repair/health` },
    { name: 'Hint Manager', url: `${SERVICES.hintManager}/api/hints/health` }
  ];

  for (const service of services) {
    healthChecks[service.name] = await makeRequest(service.url);
  }

  // Check Prometheus and Grafana
  healthChecks['Prometheus'] = await makeRequest(`${SERVICES.prometheus}/-/healthy`);
  healthChecks['Grafana'] = await makeRequest(`${SERVICES.grafana}/api/health`);

  res.json(healthChecks);
});

// Proxy endpoints for querying operations
app.post('/api/proxy/write', async (req, res) => {
  const result = await makeRequest(
    `${SERVICES.apiGateway}/api/write`,
    'POST',
    req.body
  );
  res.json(result);
});

app.get('/api/proxy/read/:partitionId/:version', async (req, res) => {
  const { partitionId, version } = req.params;
  const result = await makeRequest(
    `${SERVICES.apiGateway}/api/read/${partitionId}/${version}`
  );
  res.json(result);
});

app.post('/api/proxy/merkle/build', async (req, res) => {
  const result = await makeRequest(
    `${SERVICES.merkleTree}/api/merkle/build`,
    'POST',
    req.body
  );
  res.json(result);
});

app.post('/api/proxy/merkle/compare', async (req, res) => {
  const result = await makeRequest(
    `${SERVICES.merkleTree}/api/merkle/compare`,
    'POST',
    req.body
  );
  res.json(result);
});

app.get('/api/proxy/coordinator/jobs', async (req, res) => {
  const status = req.query.status || '';
  const url = status 
    ? `${SERVICES.coordinator}/api/coordinator/jobs?status=${status}`
    : `${SERVICES.coordinator}/api/coordinator/jobs`;
  const result = await makeRequest(url);
  res.json(result);
});

app.post('/api/proxy/coordinator/trigger', async (req, res) => {
  const result = await makeRequest(
    `${SERVICES.coordinator}/api/coordinator/trigger`,
    'POST'
  );
  res.json(result);
});

app.get('/api/proxy/hints/pending', async (req, res) => {
  const targetNode = req.query.targetNode || '';
  const url = targetNode
    ? `${SERVICES.hintManager}/api/hints/pending?targetNode=${targetNode}`
    : `${SERVICES.hintManager}/api/hints/pending`;
  const result = await makeRequest(url);
  res.json(result);
});

app.get('/api/proxy/hints/stats', async (req, res) => {
  const result = await makeRequest(`${SERVICES.hintManager}/api/hints/stats`);
  res.json(result);
});

app.get('/api/proxy/storage/:node/read/:partitionId/:version', async (req, res) => {
  const { node, partitionId, version } = req.params;
  let nodeUrl;
  if (node === '1') nodeUrl = SERVICES.node1;
  else if (node === '2') nodeUrl = SERVICES.node2;
  else if (node === '3') nodeUrl = SERVICES.node3;
  else nodeUrl = SERVICES.node1;
  
  const result = await makeRequest(
    `${nodeUrl}/api/storage/read/${partitionId}/${version}`
  );
  res.json(result);
});

app.get('/api/proxy/storage/:node/read/:partitionId/latest', async (req, res) => {
  const { node, partitionId } = req.params;
  let nodeUrl;
  if (node === '1') nodeUrl = SERVICES.node1;
  else if (node === '2') nodeUrl = SERVICES.node2;
  else if (node === '3') nodeUrl = SERVICES.node3;
  else nodeUrl = SERVICES.node1;
  
  const result = await makeRequest(
    `${nodeUrl}/api/storage/read/${partitionId}/latest`
  );
  res.json(result);
});

// Prometheus query endpoint
app.get('/api/prometheus/query', async (req, res) => {
  const query = req.query.q;
  if (!query) {
    return res.status(400).json({ error: 'Query parameter "q" is required' });
  }
  
  try {
    const response = await axios.get(`${SERVICES.prometheus}/api/v1/query`, {
      params: { query }
    });
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Prometheus query range endpoint
app.get('/api/prometheus/query_range', async (req, res) => {
  const { q, start, end, step } = req.query;
  if (!q) {
    return res.status(400).json({ error: 'Query parameter "q" is required' });
  }
  
  try {
    const response = await axios.get(`${SERVICES.prometheus}/api/v1/query_range`, {
      params: {
        query: q,
        start: start || (Date.now() / 1000 - 3600), // Default: last hour
        end: end || (Date.now() / 1000),
        step: step || '15s'
      }
    });
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get system statistics
app.get('/api/stats', async (req, res) => {
  const stats = {};
  
  // Get hint stats
  const hintStats = await makeRequest(`${SERVICES.hintManager}/api/hints/stats`);
  stats.hints = hintStats.success ? hintStats.data : null;
  
  // Get coordinator jobs
  const jobs = await makeRequest(`${SERVICES.coordinator}/api/coordinator/jobs`);
  if (jobs.success && Array.isArray(jobs.data)) {
    const jobStats = {
      total: jobs.data.length,
      pending: jobs.data.filter(j => j.status === 'PENDING').length,
      running: jobs.data.filter(j => j.status === 'RUNNING').length,
      completed: jobs.data.filter(j => j.status === 'COMPLETED').length,
      failed: jobs.data.filter(j => j.status === 'FAILED').length
    };
    stats.jobs = jobStats;
  }
  
  // Get Prometheus metrics for real-time stats
  try {
    const metrics = {};
    
    // Helper function to safely get metric value
    const getMetricValue = async (query) => {
      try {
        const result = await axios.get(`${SERVICES.prometheus}/api/v1/query`, {
          params: { query },
          timeout: 3000
        });
        if (result.data.status === 'success' && result.data.data.result && result.data.data.result.length > 0) {
          // Sum all results if multiple (e.g., from different nodes)
          const total = result.data.data.result.reduce((sum, r) => {
            return sum + (parseFloat(r.value[1]) || 0);
          }, 0);
          return Math.floor(total);
        }
        return 0;
      } catch (err) {
        return 0;
      }
    };
    
    // Storage writes (sum across all nodes)
    metrics.totalWrites = await getMetricValue('sum(storage_writes_total)');
    
    // Storage reads (sum across all nodes)
    metrics.totalReads = await getMetricValue('sum(storage_reads_total)');
    
    // Pending hints from Prometheus
    metrics.pendingHints = await getMetricValue('hints_pending');
    
    // Merkle tree comparisons
    metrics.merkleComparisons = await getMetricValue('sum(merkle_tree_comparisons_total)');
    
    // Merkle tree builds
    metrics.merkleBuilds = await getMetricValue('sum(merkle_tree_builds_total)');
    
    // Read repairs
    metrics.readRepairs = await getMetricValue('sum(read_repairs_triggered_total)');
    
    // Reconciliation jobs completed
    metrics.reconciliationJobs = await getMetricValue('sum(reconciliation_jobs_completed_total)');
    
    // Hints stored
    metrics.hintsStored = await getMetricValue('sum(hints_stored_total)');
    
    // Hints delivered
    metrics.hintsDelivered = await getMetricValue('sum(hints_delivered_total)');
    
    stats.metrics = metrics;
  } catch (error) {
    console.error('Error fetching Prometheus metrics:', error.message);
    stats.metrics = {};
  }
  
  res.json(stats);
});

// Serve dashboard
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Dashboard server running on port ${PORT}`);
  console.log(`Access dashboard at http://localhost:${PORT}`);
});

