// API base URL
const API_BASE = window.location.origin;

// Update metrics every 5 seconds
const UPDATE_INTERVAL = 5000;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    updateAllMetrics();
    checkSystemStatus();
    setInterval(updateAllMetrics, UPDATE_INTERVAL);
    setInterval(checkSystemStatus, 30000); // Check status every 30 seconds
});

// Update all metrics
async function updateAllMetrics() {
    try {
        // Get distribution metrics
        const distResponse = await fetch(`${API_BASE}/api/distribution`);
        if (distResponse.ok) {
            const distData = await distResponse.json();
            updateDistributionMetrics(distData);
        }

        // Get ingestion rate from Prometheus
        updateIngestionRate();

        // Get total logs from Prometheus
        updateTotalLogs();
    } catch (error) {
        console.error('Error updating metrics:', error);
    }
}

// Update distribution metrics
function updateDistributionMetrics(data) {
    // Balance score
    const balanceScore = data.balanceScore || 0;
    document.getElementById('balance-score').textContent = balanceScore.toFixed(2) + '%';
    
    const balanceCard = document.getElementById('balance-score-card');
    balanceCard.className = 'metric-card';
    if (balanceScore >= 95) {
        balanceCard.classList.add('good');
    } else if (balanceScore >= 85) {
        balanceCard.classList.add('warning');
    } else {
        balanceCard.classList.add('critical');
    }

    // Active nodes
    const activeNodes = data.totalNodes || 0;
    document.getElementById('active-nodes').textContent = activeNodes;
    
    const nodesCard = document.getElementById('active-nodes-card');
    nodesCard.className = 'metric-card';
    if (activeNodes >= 3) {
        nodesCard.classList.add('good');
    } else if (activeNodes >= 2) {
        nodesCard.classList.add('warning');
    } else {
        nodesCard.classList.add('critical');
    }

    // Logs per node
    const logsPerNode = data.logsPerNode || {};
    const logsPerNodeDiv = document.getElementById('logs-per-node');
    logsPerNodeDiv.innerHTML = '';
    
    Object.entries(logsPerNode).forEach(([node, count]) => {
        const nodeItem = document.createElement('div');
        nodeItem.className = 'node-log-item';
        nodeItem.innerHTML = `
            <h4>${node}</h4>
            <div class="count">${count.toLocaleString()}</div>
        `;
        logsPerNodeDiv.appendChild(nodeItem);
    });
}

// Update ingestion rate from Prometheus
async function updateIngestionRate() {
    try {
        // Try logs_ingested_total first, fallback to logs_ingested if needed
        const query = 'sum(rate(logs_ingested_total[5m])) * 60';
        const response = await fetch(`${API_BASE}/api/prometheus/query?query=${encodeURIComponent(query)}`);
        if (response.ok) {
            const data = await response.json();
            if (data.data && data.data.result && data.data.result.length > 0) {
                const value = parseFloat(data.data.result[0].value[1]);
                if (!isNaN(value) && isFinite(value)) {
                    document.getElementById('ingestion-rate').textContent = value.toFixed(1);
                } else {
                    document.getElementById('ingestion-rate').textContent = '0';
                }
            } else {
                document.getElementById('ingestion-rate').textContent = '0';
            }
        }
    } catch (error) {
        console.error('Error fetching ingestion rate:', error);
        document.getElementById('ingestion-rate').textContent = '--';
    }
}

// Update total logs from Prometheus
async function updateTotalLogs() {
    try {
        const query = 'sum(storage_node_log_count)';
        const response = await fetch(`${API_BASE}/api/prometheus/query?query=${encodeURIComponent(query)}`);
        if (response.ok) {
            const data = await response.json();
            if (data.data && data.data.result && data.data.result.length > 0) {
                const value = parseFloat(data.data.result[0].value[1]);
                if (!isNaN(value) && isFinite(value)) {
                    document.getElementById('total-logs').textContent = value.toLocaleString();
                } else {
                    document.getElementById('total-logs').textContent = '0';
                }
            } else {
                document.getElementById('total-logs').textContent = '0';
            }
        }
    } catch (error) {
        console.error('Error fetching total logs:', error);
        document.getElementById('total-logs').textContent = '--';
    }
}

// Execute Prometheus query
async function executePrometheusQuery() {
    const query = document.getElementById('prometheus-query').value;
    if (!query) {
        alert('Please enter a query');
        return;
    }

    const resultDiv = document.getElementById('prometheus-result');
    resultDiv.textContent = 'Executing query...';

    try {
        const response = await fetch(`${API_BASE}/api/prometheus/query?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        
        if (data.status === 'success') {
            resultDiv.textContent = JSON.stringify(data.data, null, 2);
        } else {
            resultDiv.textContent = `Error: ${data.error || 'Unknown error'}\n\n${JSON.stringify(data, null, 2)}`;
        }
    } catch (error) {
        resultDiv.textContent = `Error: ${error.message}`;
    }
}

// Check system status
async function checkSystemStatus() {
    // Check gateway via our API proxy
    checkGatewayStatus();
    
    // Check coordinator via our API
    checkCoordinatorStatus();
    
    // Check Prometheus
    checkPrometheusStatus();
    
    // Check Grafana
    checkGrafanaStatus();
}

// Check gateway status
async function checkGatewayStatus() {
    const element = document.getElementById('gateway-status');
    element.textContent = 'Checking...';
    element.className = 'status-indicator checking';
    
    try {
        const response = await fetch(`${API_BASE}/api/gateway/health`);
        if (response.ok) {
            const data = await response.json();
            if (data.status === 'ok') {
                element.textContent = 'Online';
                element.className = 'status-indicator online';
            } else {
                element.textContent = 'Offline';
                element.className = 'status-indicator offline';
            }
        } else {
            element.textContent = 'Offline';
            element.className = 'status-indicator offline';
        }
    } catch (error) {
        element.textContent = 'Offline';
        element.className = 'status-indicator offline';
    }
}

// Check coordinator status
async function checkCoordinatorStatus() {
    const element = document.getElementById('coordinator-status');
    element.textContent = 'Checking...';
    element.className = 'status-indicator checking';
    
    try {
        // Try health endpoint first, fallback to nodes endpoint
        const healthResponse = await fetch(`${API_BASE}/api/coordinator/health`);
        if (healthResponse.ok) {
            const data = await healthResponse.json();
            if (data.status === 'ok') {
                element.textContent = 'Online';
                element.className = 'status-indicator online';
                return;
            }
        }
        
        // Fallback to nodes endpoint
        const response = await fetch(`${API_BASE}/api/nodes`);
        if (response.ok) {
            element.textContent = 'Online';
            element.className = 'status-indicator online';
        } else {
            element.textContent = 'Offline';
            element.className = 'status-indicator offline';
        }
    } catch (error) {
        element.textContent = 'Offline';
        element.className = 'status-indicator offline';
    }
}

// Check Prometheus status
async function checkPrometheusStatus() {
    const element = document.getElementById('prometheus-status');
    element.textContent = 'Checking...';
    element.className = 'status-indicator checking';
    
    try {
        const response = await fetch(`${API_BASE}/api/prometheus/query?query=up`);
        if (response.ok) {
            element.textContent = 'Online';
            element.className = 'status-indicator online';
        } else {
            element.textContent = 'Offline';
            element.className = 'status-indicator offline';
        }
    } catch (error) {
        element.textContent = 'Offline';
        element.className = 'status-indicator offline';
    }
}

// Check Grafana status
async function checkGrafanaStatus() {
    const element = document.getElementById('grafana-status');
    element.textContent = 'Checking...';
    element.className = 'status-indicator checking';
    
    try {
        const response = await fetch('http://localhost:3000/api/health', { mode: 'no-cors' });
        // If no CORS error, it's likely online
        element.textContent = 'Online';
        element.className = 'status-indicator online';
    } catch (error) {
        // CORS error means Grafana is running but blocking CORS
        // Try to check iframe load instead
        const iframe = document.getElementById('grafana-iframe');
        if (iframe) {
            iframe.onload = () => {
                element.textContent = 'Online';
                element.className = 'status-indicator online';
            };
            iframe.onerror = () => {
                element.textContent = 'Offline';
                element.className = 'status-indicator offline';
            };
        } else {
            element.textContent = 'Unknown';
            element.className = 'status-indicator checking';
        }
    }
}


// Allow Enter key to execute query
document.getElementById('prometheus-query').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        executePrometheusQuery();
    }
});

// Demo query functions
function updateDemoQuery() {
    const select = document.getElementById('demo-query-select');
    const queryText = document.getElementById('demo-query-text');
    queryText.textContent = select.value;
    updateGraphExplanation(select.value);
}

function updateGraphExplanation(query) {
    const explanationText = document.getElementById('demo-graph-explanation-text');
    let explanation = '';
    
    if (query.includes('rate(logs_ingested_total')) {
        explanation = 'Grafana would take this data and create a time series line graph showing logs ingested per minute over time. Each data point becomes a point on the line chart, and Grafana connects them to show trends.';
    } else if (query.includes('balance_score')) {
        explanation = 'Grafana would display this as a Stat panel showing a single number with color coding (green for good, yellow/orange for warning, red for critical). It updates in real-time as the balance score changes.';
    } else if (query.includes('storage_node_log_count') && !query.includes('sum(')) {
        explanation = 'Grafana would create a Bar Gauge visualization showing horizontal bars for each node. Each bar represents the log count for that node, making it easy to compare distribution across nodes.';
    } else if (query.includes('logs_routed_total')) {
        explanation = 'Grafana would create a multi-line time series graph where each line represents a different node. This allows you to see routing patterns and compare throughput between nodes over time.';
    } else if (query.includes('count(storage_node_log_count)')) {
        explanation = 'Grafana would display this as a Stat panel showing the number of active nodes. This is typically shown as a single number with color coding based on the count (green for 3, yellow for 2, red for 1 or 0).';
    } else {
        explanation = 'Grafana would visualize this data based on the panel type configured. Time series data becomes line/bar charts, single values become stat panels, and multi-series data becomes multi-line graphs.';
    }
    
    explanationText.textContent = explanation;
}

async function executeDemoQuery() {
    const select = document.getElementById('demo-query-select');
    const query = select.value;
    const responseDiv = document.getElementById('demo-query-response');
    
    responseDiv.textContent = 'Executing query...';
    
    try {
        const response = await fetch(`${API_BASE}/api/prometheus/query?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        
        if (data.status === 'success') {
            // Format the response nicely
            const formatted = JSON.stringify(data.data, null, 2);
            responseDiv.textContent = formatted;
        } else {
            responseDiv.textContent = `Error: ${data.error || 'Unknown error'}\n\n${JSON.stringify(data, null, 2)}`;
        }
    } catch (error) {
        responseDiv.textContent = `Error: ${error.message}`;
    }
}

// Initialize demo query explanation on page load
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('demo-query-select')) {
        updateGraphExplanation(document.getElementById('demo-query-select').value);
    }
    if (document.getElementById('graph-query-select')) {
        updateDemoGraph();
        // Animate graphs after a short delay
        setTimeout(animateGraphs, 500);
    }
});

// Demo graph functions
function updateDemoGraph() {
    const select = document.getElementById('graph-query-select');
    const value = select.value;
    
    // Hide all graphs
    document.querySelectorAll('.demo-graph').forEach(graph => {
        graph.classList.remove('active');
    });
    
    // Show selected graph
    const selectedGraph = document.getElementById(`graph-${value}`);
    if (selectedGraph) {
        selectedGraph.classList.add('active');
    }
    
    // Update title and query code
    const titles = {
        'ingestion': 'Log Ingestion Rate',
        'balance': 'Distribution Balance Score',
        'logs-per-node': 'Logs per Node',
        'routing-rate': 'Routing Rate by Node',
        'active-nodes': 'Active Nodes Count'
    };
    
    const queries = {
        'ingestion': 'sum(rate(logs_ingested_total[5m])) * 60',
        'balance': 'storage_distribution_balance_score',
        'logs-per-node': 'storage_node_log_count',
        'routing-rate': 'sum(rate(logs_routed_total[1m])) by (target_node)',
        'active-nodes': 'count(storage_node_log_count)'
    };
    
    document.getElementById('graph-title').textContent = titles[value] || 'Graph';
    document.getElementById('graph-query-code').textContent = queries[value] || '';
    
    // Update stat values if needed
    if (value === 'balance') {
        updateBalanceStat();
    } else if (value === 'active-nodes') {
        updateActiveNodesStat();
    }
}

async function updateBalanceStat() {
    try {
        const response = await fetch(`${API_BASE}/api/prometheus/query?query=${encodeURIComponent('storage_distribution_balance_score')}`);
        const data = await response.json();
        if (data.data && data.data.result && data.data.result.length > 0) {
            const value = parseFloat(data.data.result[0].value[1]);
            if (!isNaN(value) && isFinite(value)) {
                document.getElementById('stat-balance-value').textContent = value.toFixed(1) + '%';
                const indicator = document.querySelector('#graph-balance .stat-threshold-indicator');
                indicator.className = 'stat-threshold-indicator';
                if (value >= 95) {
                    indicator.classList.add('good');
                    indicator.textContent = 'Excellent (95%+)';
                } else if (value >= 85) {
                    indicator.classList.add('warning');
                    indicator.textContent = 'Good (85-95%)';
                } else {
                    indicator.classList.add('critical');
                    indicator.textContent = 'Needs Improvement (<85%)';
                }
            }
        }
    } catch (error) {
        console.error('Error updating balance stat:', error);
    }
}

async function updateActiveNodesStat() {
    try {
        const response = await fetch(`${API_BASE}/api/prometheus/query?query=${encodeURIComponent('count(storage_node_log_count)')}`);
        const data = await response.json();
        if (data.data && data.data.result && data.data.result.length > 0) {
            const value = parseFloat(data.data.result[0].value[1]);
            if (!isNaN(value) && isFinite(value)) {
                document.getElementById('stat-nodes-value').textContent = value.toString();
                const indicator = document.querySelector('#graph-active-nodes .stat-threshold-indicator');
                indicator.className = 'stat-threshold-indicator';
                if (value >= 3) {
                    indicator.classList.add('good');
                    indicator.textContent = 'All nodes healthy';
                } else if (value >= 2) {
                    indicator.classList.add('warning');
                    indicator.textContent = 'Some nodes offline';
                } else {
                    indicator.classList.add('critical');
                    indicator.textContent = 'Critical: Most nodes offline';
                }
            }
        }
    } catch (error) {
        console.error('Error updating nodes stat:', error);
    }
}

// Animate graphs on load
function animateGraphs() {
    // Animate bar gauges
    const bars = document.querySelectorAll('.bar-gauge-fill');
    bars.forEach(bar => {
        const width = bar.style.width;
        bar.style.width = '0%';
        setTimeout(() => {
            bar.style.width = width;
        }, 100);
    });
}

// Update graphs periodically
setInterval(() => {
    const select = document.getElementById('graph-query-select');
    if (select && select.value === 'balance') {
        updateBalanceStat();
    } else if (select && select.value === 'active-nodes') {
        updateActiveNodesStat();
    }
}, 10000);

