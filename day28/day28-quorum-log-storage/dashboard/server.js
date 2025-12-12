const express = require('express');
const path = require('path');
const axios = require('axios');
const cors = require('cors');

const app = express();
const PORT = 8888;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Configuration
const API_GATEWAY_URL = process.env.API_GATEWAY_URL || 'http://localhost:8080';
const PROMETHEUS_URL = process.env.PROMETHEUS_URL || 'http://localhost:9090';
const GRAFANA_URL = process.env.GRAFANA_URL || 'http://localhost:3000';

// Health check endpoint
app.get('/api/health', async (req, res) => {
    try {
        const services = {
            apiGateway: false,
            prometheus: false,
            grafana: false
        };

        // Check API Gateway
        try {
            await axios.get(`${API_GATEWAY_URL}/actuator/health`, { timeout: 2000 });
            services.apiGateway = true;
        } catch (e) {}

        // Check Prometheus
        try {
            await axios.get(`${PROMETHEUS_URL}/api/v1/status/config`, { timeout: 2000 });
            services.prometheus = true;
        } catch (e) {}

        // Check Grafana - try multiple endpoints
        try {
            await axios.get(`${GRAFANA_URL}/api/health`, { timeout: 2000 });
            services.grafana = true;
        } catch (e) {
            try {
                // Try login page as fallback
                await axios.get(`${GRAFANA_URL}/login`, { timeout: 2000, validateStatus: () => true });
                services.grafana = true;
            } catch (e2) {
                services.grafana = false;
            }
        }

        res.json({ services, timestamp: new Date().toISOString() });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Proxy for API Gateway operations
app.post('/api/operation/write', async (req, res) => {
    try {
        const { key, value, consistency } = req.body;
        const consistencyParam = consistency || 'QUORUM';
        
        if (!key || !value) {
            return res.status(400).json({
                success: false,
                error: 'Key and value are required',
                timestamp: new Date().toISOString()
            });
        }
        
        const response = await axios.post(
            `${API_GATEWAY_URL}/api/logs?consistency=${consistencyParam}`,
            { key, value },
            { 
                headers: { 'Content-Type': 'application/json' },
                timeout: 30000
            }
        );
        
        res.json({
            success: true,
            data: response.data,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        const status = error.response?.status || 500;
        const errorData = error.response?.data || {};
        const errorMessage = errorData.message || errorData.error || error.message || 'Unknown error occurred';
        
        console.error('Write operation error:', {
            status,
            error: errorMessage,
            details: errorData,
            stack: error.stack
        });
        
        res.status(status).json({
            success: false,
            error: errorMessage,
            details: errorData,
            timestamp: new Date().toISOString()
        });
    }
});

app.get('/api/operation/read/:key', async (req, res) => {
    try {
        const { key } = req.params;
        const { consistency = 'QUORUM' } = req.query;
        
        if (!key) {
            return res.status(400).json({
                success: false,
                error: 'Key is required',
                timestamp: new Date().toISOString()
            });
        }
        
        const response = await axios.get(
            `${API_GATEWAY_URL}/api/logs/${key}?consistency=${consistency}`,
            { timeout: 30000 }
        );
        
        res.json({
            success: true,
            data: response.data,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        const status = error.response?.status || 500;
        const errorData = error.response?.data || {};
        const errorMessage = errorData.message || errorData.error || error.message || 'Unknown error occurred';
        
        // 404 is expected for non-existent keys, don't log as error
        if (status === 404) {
            return res.status(404).json({
                success: false,
                error: 'Key not found',
                timestamp: new Date().toISOString()
            });
        }
        
        console.error('Read operation error:', {
            status,
            error: errorMessage,
            details: errorData,
            stack: error.stack
        });
        
        res.status(status).json({
            success: false,
            error: errorMessage,
            details: errorData,
            timestamp: new Date().toISOString()
        });
    }
});

// Prometheus query proxy
app.post('/api/prometheus/query', async (req, res) => {
    try {
        const { query } = req.body;
        
        const response = await axios.get(`${PROMETHEUS_URL}/api/v1/query`, {
            params: { query },
            timeout: 10000
        });
        
        res.json({
            success: true,
            data: response.data,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.response?.data?.error || error.message,
            timestamp: new Date().toISOString()
        });
    }
});

// Prometheus query range (for graphs)
app.post('/api/prometheus/query_range', async (req, res) => {
    try {
        const { query, start, end, step } = req.body;
        
        const response = await axios.get(`${PROMETHEUS_URL}/api/v1/query_range`, {
            params: {
                query,
                start: start || Math.floor(Date.now() / 1000) - 3600, // Last hour
                end: end || Math.floor(Date.now() / 1000),
                step: step || '15s'
            },
            timeout: 10000
        });
        
        res.json({
            success: true,
            data: response.data,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.response?.data?.error || error.message,
            timestamp: new Date().toISOString()
        });
    }
});

// Get predefined Prometheus queries
app.get('/api/prometheus/queries', (req, res) => {
    const queries = {
        requestRate: 'rate(http_server_requests_seconds_count{job="api-gateway"}[5m])',
        writeLatency: 'rate(http_server_requests_seconds_sum{job="api-gateway", uri="/api/logs", method="POST"}[5m]) / rate(http_server_requests_seconds_count{job="api-gateway", uri="/api/logs", method="POST"}[5m]) * 1000',
        readLatency: 'rate(http_server_requests_seconds_sum{job="api-gateway", uri=~"/api/logs/.*", method="GET"}[5m]) / rate(http_server_requests_seconds_count{job="api-gateway", uri=~"/api/logs/.*", method="GET"}[5m]) * 1000',
        errorRate: 'rate(http_server_requests_seconds_count{job="api-gateway", status=~"4..|5.."}[5m])',
        successRate: '(rate(http_server_requests_seconds_count{job="api-gateway", status=~"2.."}[5m]) / rate(http_server_requests_seconds_count{job="api-gateway"}[5m])) * 100',
        quorumOperations: 'rate(http_server_requests_seconds_count{job="quorum-coordinator", uri=~"/quorum/(write|read)"}[5m])',
        quorumLatency: 'rate(http_server_requests_seconds_sum{job="quorum-coordinator", uri=~"/quorum/(write|read)"}[5m]) / rate(http_server_requests_seconds_count{job="quorum-coordinator", uri=~"/quorum/(write|read)"}[5m]) * 1000',
        circuitBreakerState: 'resilience4j_circuitbreaker_state{job="quorum-coordinator", name="storageNode"}',
        storageNodeRequests: 'rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])',
        jvmMemory: '(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100',
        totalThroughput: 'sum(rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/write"}[5m]))',
        activeNodes: 'count(count by (instance) (rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])))'
    };
    
    res.json({ queries });
});

// Serve dashboard
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
    console.log(`Dashboard server running on http://localhost:${PORT}`);
    console.log(`API Gateway: ${API_GATEWAY_URL}`);
    console.log(`Prometheus: ${PROMETHEUS_URL}`);
    console.log(`Grafana: ${GRAFANA_URL}`);
});

