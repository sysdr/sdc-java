// Chart instances
let replicationChart = null;
let gatewayChart = null;
let latencyChart = null;

// Initialize dashboard
document.addEventListener('DOMContentLoaded', () => {
    loadHealthStatus();
    loadTopology();
    initializeCharts();
    startAutoRefresh();
});

// Auto-refresh every 5 seconds
function startAutoRefresh() {
    setInterval(() => {
        loadHealthStatus();
        loadTopology();
        updateCharts();
    }, 5000);
}

// Load health status for all services
async function loadHealthStatus() {
    try {
        const response = await fetch('/api/health');
        const data = await response.json();
        
        updateStatusBadge('coordinatorStatus', data.coordinator);
        updateStatusBadge('node1Status', data.node1);
        updateStatusBadge('node2Status', data.node2);
        updateStatusBadge('node3Status', data.node3);
        updateStatusBadge('writeGatewayStatus', data.writeGateway);
        updateStatusBadge('readGatewayStatus', data.readGateway);
    } catch (error) {
        console.error('Error loading health status:', error);
    }
}

function updateStatusBadge(elementId, healthData) {
    const element = document.getElementById(elementId);
    if (healthData.success && healthData.data?.status === 'UP') {
        element.textContent = 'Healthy';
        element.className = 'status-badge healthy';
    } else {
        element.textContent = 'Unhealthy';
        element.className = 'status-badge unhealthy';
    }
}

// Load cluster topology
async function loadTopology() {
    try {
        const response = await fetch('/api/topology');
        const data = await response.json();
        
        if (data.success) {
            displayTopology(data.data);
        } else {
            document.getElementById('topologyInfo').textContent = 'Error loading topology: ' + data.error;
        }
    } catch (error) {
        document.getElementById('topologyInfo').textContent = 'Error: ' + error.message;
    }
}

function displayTopology(topology) {
    const infoDiv = document.getElementById('topologyInfo');
    const visualDiv = document.getElementById('topologyVisual');
    
    const nodeCount = topology.nodes?.length || 0;
    const hasNodes = nodeCount > 0;
    
    // Display topology info
    let infoHtml = `
        <strong>Cluster Generation:</strong> ${topology.generationId || 'N/A'}<br>
        <strong>Active Nodes:</strong> ${nodeCount}<br>
        <strong>Leader:</strong> ${topology.leaderId || 'None'}<br><br>
    `;
    
    if (hasNodes) {
        infoHtml += `<strong>Nodes:</strong><br>
        ${JSON.stringify(topology.nodes.map(n => typeof n === 'string' ? n : n.nodeId || n), null, 2)}`;
    } else {
        infoHtml += `<span style="color: #e74c3c; font-weight: bold;">⚠️ No nodes registered!</span><br><br>
        The cluster coordinator cannot find any registered storage nodes.<br>
        This may indicate:<br>
        • Storage nodes are still starting up<br>
        • Heartbeat registration issues<br>
        • Network connectivity problems<br><br>
        Check the storage node logs for errors.`;
    }
    
    infoDiv.innerHTML = infoHtml;
    
    // Visual representation
    visualDiv.innerHTML = '';
    if (hasNodes) {
        topology.nodes.forEach((node, index) => {
            const nodeDiv = document.createElement('div');
            const nodeId = typeof node === 'string' ? node : (node.nodeId || 'unknown');
            nodeDiv.className = 'node-visual' + (index === 0 ? ' leader' : '');
            nodeDiv.textContent = nodeId;
            visualDiv.appendChild(nodeDiv);
        });
    } else {
        visualDiv.innerHTML = '<div style="color: #e74c3c; font-weight: bold;">⚠️ No nodes available</div>';
    }
}

// Perform write operation
async function performWrite() {
    const key = document.getElementById('writeKey').value;
    const content = document.getElementById('writeContent').value;
    const resultDiv = document.getElementById('writeResult');
    
    if (!key || !content) {
        resultDiv.textContent = 'Please provide both key and content';
        resultDiv.style.color = '#e74c3c';
        return;
    }
    
    resultDiv.textContent = 'Writing...';
    resultDiv.style.color = '#2c3e50';
    
    try {
        const response = await fetch('/api/write', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key, content })
        });
        
        const data = await response.json();
        
        if (data.success) {
            resultDiv.textContent = JSON.stringify(data, null, 2);
            resultDiv.style.color = '#27ae60';
        } else {
            resultDiv.textContent = 'Write Failed:\n' + JSON.stringify(data, null, 2) + 
                '\n\nNote: If you see a 503 error, the cluster may not be ready yet.\n' +
                'Check that storage nodes are registered in the Cluster Topology section above.';
            resultDiv.style.color = '#e74c3c';
        }
        
        // Refresh metrics after write
        setTimeout(updateCharts, 1000);
    } catch (error) {
        resultDiv.textContent = 'Error: ' + error.message;
        resultDiv.style.color = '#e74c3c';
    }
}

// Perform read operation
async function performRead() {
    const key = document.getElementById('readKey').value;
    const resultDiv = document.getElementById('readResult');
    
    if (!key) {
        resultDiv.textContent = 'Please provide a key';
        return;
    }
    
    resultDiv.textContent = 'Reading...';
    
    try {
        const response = await fetch(`/api/read/${encodeURIComponent(key)}`);
        const data = await response.json();
        resultDiv.textContent = JSON.stringify(data, null, 2);
    } catch (error) {
        resultDiv.textContent = 'Error: ' + error.message;
    }
}

// Execute Prometheus query
async function executeQuery() {
    const query = document.getElementById('prometheusQuery').value;
    const resultDiv = document.getElementById('queryResult');
    
    if (!query) {
        resultDiv.textContent = 'Please enter a query';
        return;
    }
    
    resultDiv.textContent = 'Querying...';
    
    try {
        const response = await fetch(`/api/prometheus/query?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        
        if (data.success) {
            resultDiv.textContent = JSON.stringify(data.data, null, 2);
        } else {
            resultDiv.textContent = 'Error: ' + data.error;
        }
    } catch (error) {
        resultDiv.textContent = 'Error: ' + error.message;
    }
}

// Load metric into query and chart
function loadMetric(metricName) {
    document.getElementById('prometheusQuery').value = metricName;
    executeQuery();
    updateCharts();
}

// Initialize charts
function initializeCharts() {
    const chartOptions = {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
            legend: {
                display: true,
                position: 'top'
            }
        },
        scales: {
            y: {
                beginAtZero: true
            }
        }
    };
    
    // Replication Chart
    const replicationCtx = document.getElementById('replicationChart').getContext('2d');
    replicationChart = new Chart(replicationCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Replication Success',
                    data: [],
                    borderColor: '#27ae60',
                    backgroundColor: 'rgba(39, 174, 96, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Replication Failures',
                    data: [],
                    borderColor: '#e74c3c',
                    backgroundColor: 'rgba(231, 76, 60, 0.1)',
                    tension: 0.4
                }
            ]
        },
        options: chartOptions
    });
    
    // Gateway Chart
    const gatewayCtx = document.getElementById('gatewayChart').getContext('2d');
    gatewayChart = new Chart(gatewayCtx, {
        type: 'bar',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Write Success',
                    data: [],
                    backgroundColor: '#f39c12',
                    borderColor: '#e67e22',
                    borderWidth: 1
                },
                {
                    label: 'Read Success',
                    data: [],
                    backgroundColor: '#27ae60',
                    borderColor: '#229954',
                    borderWidth: 1
                }
            ]
        },
        options: chartOptions
    });
    
    // Latency Chart
    const latencyCtx = document.getElementById('latencyChart').getContext('2d');
    latencyChart = new Chart(latencyCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Write Latency (ms)',
                    data: [],
                    borderColor: '#f39c12',
                    backgroundColor: 'rgba(243, 156, 18, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Read Latency (ms)',
                    data: [],
                    borderColor: '#27ae60',
                    backgroundColor: 'rgba(39, 174, 96, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Replication Latency (ms)',
                    data: [],
                    borderColor: '#2c3e50',
                    backgroundColor: 'rgba(44, 62, 80, 0.1)',
                    tension: 0.4
                }
            ]
        },
        options: chartOptions
    });
    
    // Initial chart update
    updateCharts();
}

// Update charts with latest metrics
async function updateCharts() {
    const now = Math.floor(Date.now() / 1000);
    const oneHourAgo = now - 3600;
    
    try {
        // Update replication metrics
        await updateChartData(
            replicationChart,
            'storage_replication_success_total',
            'storage_replication_failure_total',
            oneHourAgo,
            now,
            0,
            1
        );
        
        // Update gateway metrics
        await updateChartData(
            gatewayChart,
            'gateway_write_success_total',
            'gateway_read_success_total',
            oneHourAgo,
            now,
            0,
            1
        );
        
        // Update latency metrics
        await updateLatencyChart(latencyChart, oneHourAgo, now);
    } catch (error) {
        console.error('Error updating charts:', error);
    }
}

// Update chart with query range data
async function updateChartData(chart, metric1, metric2, start, end, dataset1Index, dataset2Index) {
    try {
        const [data1, data2] = await Promise.all([
            fetchQueryRange(metric1, start, end),
            fetchQueryRange(metric2, start, end)
        ]);
        
        if (data1 && data1.result && data1.result.length > 0) {
            const series = data1.result[0];
            const labels = series.values.map(v => new Date(v[0] * 1000).toLocaleTimeString());
            const values = series.values.map(v => parseFloat(v[1]));
            
            chart.data.labels = labels;
            chart.data.datasets[dataset1Index].data = values;
        }
        
        if (data2 && data2.result && data2.result.length > 0) {
            const series = data2.result[0];
            const values = series.values.map(v => parseFloat(v[1]));
            chart.data.datasets[dataset2Index].data = values;
        }
        
        chart.update('none');
    } catch (error) {
        console.error(`Error updating chart for ${metric1}/${metric2}:`, error);
    }
}

// Update latency chart
async function updateLatencyChart(chart, start, end) {
    try {
        // Use max values for latency metrics (they represent the maximum latency observed)
        // For better visualization, we'll use max values which reset periodically
        const [writeData, readData, replicationData] = await Promise.all([
            fetchQueryRange('gateway_write_latency_seconds_max', start, end),
            fetchQueryRange('gateway_read_latency_seconds_max', start, end),
            fetchQueryRange('storage_replication_latency_seconds_max', start, end)
        ]);
        
        let labels = [];
        let writeValues = [];
        let readValues = [];
        let replicationValues = [];
        
        // Process write latency
        if (writeData && writeData.result && writeData.result.length > 0) {
            const series = writeData.result[0];
            labels = series.values.map(v => new Date(v[0] * 1000).toLocaleTimeString());
            writeValues = series.values.map(v => {
                const val = parseFloat(v[1]);
                return isNaN(val) ? 0 : val * 1000; // Convert to ms
            });
        }
        
        // Process read latency
        if (readData && readData.result && readData.result.length > 0) {
            const series = readData.result[0];
            if (labels.length === 0) {
                labels = series.values.map(v => new Date(v[0] * 1000).toLocaleTimeString());
            }
            readValues = series.values.map(v => {
                const val = parseFloat(v[1]);
                return isNaN(val) ? 0 : val * 1000; // Convert to ms
            });
        }
        
        // Process replication latency
        if (replicationData && replicationData.result && replicationData.result.length > 0) {
            const series = replicationData.result[0];
            if (labels.length === 0) {
                labels = series.values.map(v => new Date(v[0] * 1000).toLocaleTimeString());
            }
            replicationValues = series.values.map(v => {
                const val = parseFloat(v[1]);
                return isNaN(val) ? 0 : val * 1000; // Convert to ms
            });
        }
        
        // If no historical data, try to get current values and create a simple chart
        if (labels.length === 0) {
            try {
                const [writeCurrent, readCurrent, replCurrent] = await Promise.all([
                    fetch('/api/prometheus/query?query=' + encodeURIComponent('gateway_write_latency_seconds_max')),
                    fetch('/api/prometheus/query?query=' + encodeURIComponent('gateway_read_latency_seconds_max')),
                    fetch('/api/prometheus/query?query=' + encodeURIComponent('storage_replication_latency_seconds_max'))
                ]);
                
                const writeResult = await writeCurrent.json();
                const readResult = await readCurrent.json();
                const replResult = await replCurrent.json();
                
                const now = new Date();
                labels = [now.toLocaleTimeString()];
                
                if (writeResult.success && writeResult.data?.data?.result?.[0]) {
                    writeValues = [parseFloat(writeResult.data.data.result[0].value[1]) * 1000];
                }
                if (readResult.success && readResult.data?.data?.result?.[0]) {
                    readValues = [parseFloat(readResult.data.data.result[0].value[1]) * 1000];
                }
                if (replResult.success && replResult.data?.data?.result?.[0]) {
                    replicationValues = [parseFloat(replResult.data.data.result[0].value[1]) * 1000];
                }
            } catch (err) {
                console.error('Error fetching current latency values:', err);
            }
        }
        
        // Update chart if we have any data
        if (labels.length > 0) {
            chart.data.labels = labels;
            chart.data.datasets[0].data = writeValues;
            chart.data.datasets[1].data = readValues;
            chart.data.datasets[2].data = replicationValues;
            chart.update('none');
        }
    } catch (error) {
        console.error('Error updating latency chart:', error);
    }
}

// Fetch query range from Prometheus
async function fetchQueryRange(query, start, end) {
    try {
        const response = await fetch(
            `/api/prometheus/query_range?query=${encodeURIComponent(query)}&start=${start}&end=${end}&step=15s`
        );
        const result = await response.json();
        
        if (result.success && result.data?.data) {
            return result.data.data;
        }
        return null;
    } catch (error) {
        console.error('Error fetching query range:', error);
        return null;
    }
}

