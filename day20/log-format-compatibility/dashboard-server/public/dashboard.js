const API_BASE = 'http://localhost:8085/api';
let charts = {};
let ws = null;

// Initialize dashboard
document.addEventListener('DOMContentLoaded', () => {
    initializeCharts();
    connectWebSocket();
    loadInitialData();
    loadKafkaTopics();
    
    // Auto-refresh every 30 seconds
    setInterval(loadInitialData, 30000);
    setInterval(loadKafkaTopics, 30000);
});

// Initialize Chart.js charts
function initializeCharts() {
    const chartOptions = {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
            legend: {
                display: true,
                position: 'top',
                labels: {
                    color: '#b0b0b0'
                }
            }
        },
        scales: {
            x: {
                ticks: {
                    color: '#b0b0b0'
                },
                grid: {
                    color: '#2a2f3e'
                }
            },
            y: {
                beginAtZero: true,
                ticks: {
                    color: '#b0b0b0'
                },
                grid: {
                    color: '#2a2f3e'
                }
            }
        }
    };

    charts.ingestion = new Chart(document.getElementById('ingestionChart'), {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Syslog Messages/sec',
                data: [],
                borderColor: 'rgb(0, 217, 255)',
                backgroundColor: 'rgba(0, 217, 255, 0.1)',
                tension: 0.4
            }, {
                label: 'Journald Messages/sec',
                data: [],
                borderColor: 'rgb(255, 167, 38)',
                backgroundColor: 'rgba(255, 167, 38, 0.1)',
                tension: 0.4
            }]
        },
        options: chartOptions
    });

    charts.normalization = new Chart(document.getElementById('normalizationChart'), {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Normalized Events/sec',
                data: [],
                borderColor: 'rgb(0, 217, 255)',
                backgroundColor: 'rgba(0, 217, 255, 0.1)',
                tension: 0.4
            }]
        },
        options: chartOptions
    });

    charts.error = new Chart(document.getElementById('errorChart'), {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Errors/sec',
                data: [],
                borderColor: 'rgb(239, 83, 80)',
                backgroundColor: 'rgba(239, 83, 80, 0.1)',
                tension: 0.4
            }]
        },
        options: chartOptions
    });

    charts.memory = new Chart(document.getElementById('memoryChart'), {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Memory Usage (MB)',
                data: [],
                borderColor: 'rgb(255, 167, 38)',
                backgroundColor: 'rgba(255, 167, 38, 0.1)',
                tension: 0.4
            }]
        },
        options: chartOptions
    });
}

// Connect WebSocket for real-time updates
function connectWebSocket() {
    ws = new WebSocket('ws://localhost:8085');
    
    ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        
        if (message.type === 'health') {
            updateHealthStatus(message.data);
        } else if (message.type === 'stats') {
            updateStats(message.data);
        }
    };
    
    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
    };
    
    ws.onclose = () => {
        console.log('WebSocket closed, reconnecting...');
        setTimeout(connectWebSocket, 5000);
    };
}

// Load initial data
async function loadInitialData() {
    try {
        // Load health status
        const healthResponse = await fetch(`${API_BASE}/health`);
        const health = await healthResponse.json();
        updateHealthStatus(health);
        
        // Load statistics
        try {
            const statsResponse = await fetch(`${API_BASE}/stats`);
            if (statsResponse.ok) {
                const stats = await statsResponse.json();
                updateStats(stats);
            }
        } catch (e) {
            console.log('Stats not available:', e);
            // Set default values and fetch from Prometheus
            updateStats({ total: 0, syslog: 0, journald: 0, normalized: 0 });
        }
        
        // Always try to load stats from Prometheus as well (in case API doesn't have source breakdown)
        loadStatsFromPrometheus();
        
        // Load metrics
        loadMetrics();
    } catch (error) {
        console.error('Error loading initial data:', error);
    }
}

// Load statistics from Prometheus metrics
async function loadStatsFromPrometheus() {
    try {
        // Fetch syslog messages
        const syslogResponse = await fetch(`${API_BASE}/prometheus/query?query=syslog_messages_produced_total`);
        if (syslogResponse.ok) {
            const syslogData = await syslogResponse.json();
            const result = syslogData.data?.result || [];
            if (result.length > 0) {
                const value = parseInt(result[0].value[1]) || 0;
                document.getElementById('stat-syslog').textContent = formatNumber(value);
            }
        }
        
        // Fetch journald messages
        const journaldResponse = await fetch(`${API_BASE}/prometheus/query?query=journald_messages_produced_total`);
        if (journaldResponse.ok) {
            const journaldData = await journaldResponse.json();
            const result = journaldData.data?.result || [];
            if (result.length > 0) {
                const value = parseInt(result[0].value[1]) || 0;
                document.getElementById('stat-journald').textContent = formatNumber(value);
            }
        }
        
        // Fetch normalized events
        const normalizedResponse = await fetch(`${API_BASE}/prometheus/query?query=normalizer_events_processed_total`);
        if (normalizedResponse.ok) {
            const normalizedData = await normalizedResponse.json();
            const result = normalizedData.data?.result || [];
            if (result.length > 0) {
                const value = parseInt(result[0].value[1]) || 0;
                document.getElementById('stat-normalized').textContent = formatNumber(value);
                document.getElementById('stat-total').textContent = formatNumber(value);
            }
        }
    } catch (error) {
        console.log('Could not load stats from Prometheus:', error);
    }
}

// Update health status
function updateHealthStatus(health) {
    const statusMap = {
        'syslogAdapter': 'status-syslog',
        'journaldAdapter': 'status-journald',
        'formatNormalizer': 'status-normalizer',
        'apiGateway': 'status-gateway'
    };
    
    let allDown = true;
    
    Object.entries(statusMap).forEach(([key, elementId]) => {
        const element = document.getElementById(elementId);
        if (element && health[key]) {
            const status = health[key].status;
            element.textContent = status;
            element.className = 'status-badge ' + status.toLowerCase();
            
            if (status === 'UP') {
                allDown = false;
            }
        }
    });
    
    // Show helpful message if all services are down
    const servicesSection = document.querySelector('.architecture-grid').parentElement;
    let helpMessage = servicesSection.querySelector('.services-help-message');
    
    if (allDown && !helpMessage) {
        helpMessage = document.createElement('div');
        helpMessage.className = 'services-help-message';
        helpMessage.innerHTML = `
            <div style="background: #2a2f3e; border: 1px solid #ffa726; border-radius: 8px; padding: 20px; margin-top: 20px;">
                <h3 style="color: #ffa726; margin-bottom: 10px;">Services Not Running</h3>
                <p style="color: #b0b0b0; margin-bottom: 15px;">To start the services, run these commands in separate terminals:</p>
                <div style="background: #1a1f2e; padding: 15px; border-radius: 5px; font-family: monospace; font-size: 0.9em; border: 1px solid #2a2f3e;">
                    <div style="margin-bottom: 10px; color: #00d9ff;"><strong>Quick Start (all services):</strong></div>
                    <div style="margin-bottom: 8px; padding-left: 10px; color: #e4e4e4;">./start-services.sh</div>
                    <div style="margin-top: 15px; margin-bottom: 10px; color: #00d9ff;"><strong>Or start individually:</strong></div>
                    <div style="margin-bottom: 8px; padding-left: 10px; color: #e4e4e4;"><strong>Terminal 1:</strong> java -jar syslog-adapter/target/syslog-adapter-1.0.0.jar</div>
                    <div style="margin-bottom: 8px; padding-left: 10px; color: #e4e4e4;"><strong>Terminal 2:</strong> java -jar journald-adapter/target/journald-adapter-1.0.0.jar</div>
                    <div style="margin-bottom: 8px; padding-left: 10px; color: #e4e4e4;"><strong>Terminal 3:</strong> java -jar format-normalizer/target/format-normalizer-1.0.0.jar</div>
                    <div style="padding-left: 10px; color: #e4e4e4;"><strong>Terminal 4:</strong> java -jar api-gateway/target/api-gateway-1.0.0.jar</div>
                </div>
                <p style="color: #b0b0b0; margin-top: 15px; font-size: 0.9em;">
                    <strong>Note:</strong> Make sure to build the services first with: <code style="background: #1a1f2e; color: #00d9ff; padding: 2px 6px; border-radius: 3px; border: 1px solid #2a2f3e;">mvn clean install</code>
                </p>
            </div>
        `;
        servicesSection.appendChild(helpMessage);
    } else if (!allDown && helpMessage) {
        helpMessage.remove();
    }
}

// Update statistics
function updateStats(stats) {
    // Try to get from stats API first
    let total = stats.total || stats.totalLogs || 0;
    let syslog = stats.syslog || 0;
    let journald = stats.journald || 0;
    let normalized = stats.normalized || stats.totalLogs || 0;
    
    // If stats don't have source breakdown, fetch from Prometheus
    if ((!stats.syslog && !stats.journald) || total === 0) {
        // Fetch from Prometheus metrics as fallback
        fetch(`${API_BASE}/prometheus/query?query=syslog_messages_produced_total`)
            .then(r => r.json())
            .then(data => {
                const result = data.data?.result || [];
                if (result.length > 0) {
                    syslog = parseInt(result[0].value[1]) || 0;
                    document.getElementById('stat-syslog').textContent = formatNumber(syslog);
                }
            })
            .catch(e => console.log('Could not fetch syslog metrics'));
        
        fetch(`${API_BASE}/prometheus/query?query=journald_messages_produced_total`)
            .then(r => r.json())
            .then(data => {
                const result = data.data?.result || [];
                if (result.length > 0) {
                    journald = parseInt(result[0].value[1]) || 0;
                    document.getElementById('stat-journald').textContent = formatNumber(journald);
                }
            })
            .catch(e => console.log('Could not fetch journald metrics'));
        
        fetch(`${API_BASE}/prometheus/query?query=normalizer_events_processed_total`)
            .then(r => r.json())
            .then(data => {
                const result = data.data?.result || [];
                if (result.length > 0) {
                    normalized = parseInt(result[0].value[1]) || 0;
                    total = normalized; // Use normalized as total
                    document.getElementById('stat-normalized').textContent = formatNumber(normalized);
                    document.getElementById('stat-total').textContent = formatNumber(total);
                }
            })
            .catch(e => console.log('Could not fetch normalized metrics'));
    } else {
        // Use API stats if available
        if (total !== undefined) {
            document.getElementById('stat-total').textContent = formatNumber(total);
        }
        if (syslog !== undefined) {
            document.getElementById('stat-syslog').textContent = formatNumber(syslog);
        }
        if (journald !== undefined) {
            document.getElementById('stat-journald').textContent = formatNumber(journald);
        }
        if (normalized !== undefined) {
            document.getElementById('stat-normalized').textContent = formatNumber(normalized);
        }
    }
}

// Load metrics from Prometheus
async function loadMetrics() {
    try {
        const now = Math.floor(Date.now() / 1000);
        const start = now - 3600; // Last hour
        
        // Syslog ingestion rate
        try {
            const syslogResponse = await fetch(
                `${API_BASE}/prometheus/query_range?query=rate(syslog_messages_produced_total[5m])&start=${start}&end=${now}&step=30s`
            );
            const syslogData = await syslogResponse.json();
            updateChartData(charts.ingestion, syslogData, 0);
        } catch (e) {
            console.log('Syslog metrics not available');
        }
        
        // Journald ingestion rate
        try {
            const journaldResponse = await fetch(
                `${API_BASE}/prometheus/query_range?query=rate(journald_messages_produced_total[5m])&start=${start}&end=${now}&step=30s`
            );
            const journaldData = await journaldResponse.json();
            updateChartData(charts.ingestion, journaldData, 1);
        } catch (e) {
            console.log('Journald metrics not available');
        }
        
        // Normalization rate
        try {
            const normResponse = await fetch(
                `${API_BASE}/prometheus/query_range?query=rate(normalizer_events_processed_total[5m])&start=${start}&end=${now}&step=30s`
            );
            const normData = await normResponse.json();
            updateChartData(charts.normalization, normData, 0);
        } catch (e) {
            console.log('Normalization metrics not available');
        }
        
        // Error rate
        try {
            const errorResponse = await fetch(
                `${API_BASE}/prometheus/query_range?query=rate(normalizer_errors_total[5m])&start=${start}&end=${now}&step=30s`
            );
            const errorData = await errorResponse.json();
            updateChartData(charts.error, errorData, 0);
        } catch (e) {
            console.log('Error metrics not available');
        }
        
        // Memory usage
        try {
            const memoryResponse = await fetch(
                `${API_BASE}/prometheus/query_range?query=jvm_memory_used_bytes/1024/1024&start=${start}&end=${now}&step=30s`
            );
            const memoryData = await memoryResponse.json();
            updateChartData(charts.memory, memoryData, 0);
        } catch (e) {
            console.log('Memory metrics not available');
        }
    } catch (error) {
        console.error('Error loading metrics:', error);
    }
}

// Update chart data
function updateChartData(chart, prometheusData, datasetIndex) {
    if (!prometheusData.data || !prometheusData.data.result || prometheusData.data.result.length === 0) {
        return;
    }
    
    const result = prometheusData.data.result[0];
    if (!result.values || result.values.length === 0) {
        return;
    }
    
    const labels = [];
    const values = [];
    
    result.values.forEach(([timestamp, value]) => {
        const date = new Date(timestamp * 1000);
        labels.push(date.toLocaleTimeString());
        values.push(parseFloat(value));
    });
    
    if (datasetIndex === 0) {
        chart.data.labels = labels;
    }
    chart.data.datasets[datasetIndex].data = values;
    chart.update('none');
}

// Execute Prometheus query
async function executePrometheusQuery() {
    const query = document.getElementById('prometheus-query').value;
    if (!query) {
        alert('Please enter a query');
        return;
    }
    
    const resultsDiv = document.getElementById('prometheus-results');
    resultsDiv.textContent = 'Executing query...';
    
    try {
        const response = await fetch(`${API_BASE}/prometheus/query?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        
        if (data.data && data.data.result) {
            resultsDiv.textContent = JSON.stringify(data.data.result, null, 2);
        } else {
            resultsDiv.textContent = JSON.stringify(data, null, 2);
        }
    } catch (error) {
        resultsDiv.textContent = `Error: ${error.message}`;
    }
}

// Query logs
async function queryLogs() {
    const level = document.getElementById('filter-level').value;
    const source = document.getElementById('filter-source').value;
    const hostname = document.getElementById('filter-hostname').value;
    const limit = document.getElementById('filter-limit').value;
    
    const params = new URLSearchParams();
    if (level) params.append('level', level);
    if (source) params.append('source', source);
    if (hostname) params.append('hostname', hostname);
    params.append('limit', limit);
    
    const tbody = document.getElementById('logs-tbody');
    tbody.innerHTML = '<tr><td colspan="5" class="no-logs">Loading...</td></tr>';
    
    try {
        const response = await fetch(`${API_BASE}/logs/search?${params}`);
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ error: `HTTP ${response.status}: ${response.statusText}` }));
            tbody.innerHTML = `<tr><td colspan="5" class="no-logs">Error: ${errorData.error || response.statusText}</td></tr>`;
            document.getElementById('logs-count').textContent = '0 logs';
            return;
        }
        
        const data = await response.json();
        
        // Check if response is an array, if not handle error object
        if (!Array.isArray(data)) {
            if (data.error) {
                tbody.innerHTML = `<tr><td colspan="5" class="no-logs">Error: ${data.error}</td></tr>`;
            } else {
                tbody.innerHTML = '<tr><td colspan="5" class="no-logs">Error: Invalid response format</td></tr>';
            }
            document.getElementById('logs-count').textContent = '0 logs';
            return;
        }
        
        const logs = data;
        
        if (logs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="no-logs">No logs found</td></tr>';
            document.getElementById('logs-count').textContent = '0 logs';
            return;
        }
        
        tbody.innerHTML = '';
        logs.forEach(log => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${formatTimestamp(log.timestamp)}</td>
                <td><span class="level-badge level-${log.level?.toLowerCase()}">${log.level || 'N/A'}</span></td>
                <td>${log.source || 'N/A'}</td>
                <td>${log.hostname || 'N/A'}</td>
                <td>${log.message || log.rawMessage || 'N/A'}</td>
            `;
            tbody.appendChild(row);
        });
        
        document.getElementById('logs-count').textContent = `${logs.length} logs`;
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="5" class="no-logs">Error: ${error.message}</td></tr>`;
        document.getElementById('logs-count').textContent = '0 logs';
    }
}

// Load Kafka topics
async function loadKafkaTopics() {
    try {
        const response = await fetch(`${API_BASE}/kafka/topics`);
        const data = await response.json();
        
        const topicsDiv = document.getElementById('kafka-topics');
        if (data.topics && data.topics.length > 0) {
            topicsDiv.innerHTML = '<ul>' + data.topics.map(topic => `<li>${topic}</li>`).join('') + '</ul>';
        } else {
            topicsDiv.innerHTML = '<p>No topics found</p>';
        }
    } catch (error) {
        document.getElementById('kafka-topics').innerHTML = `<p>Error loading topics: ${error.message}</p>`;
    }
}

// Tab switching
function showTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });
    
    document.getElementById(`${tabName}-tab`).classList.add('active');
    event?.target?.classList.add('active');
    
    // Also activate the button that was clicked
    document.querySelectorAll('.tab-button').forEach(btn => {
        if (btn.textContent.toLowerCase() === tabName) {
            btn.classList.add('active');
        }
    });
}

// Handle Grafana iframe errors
function handleGrafanaError() {
    const iframe = document.getElementById('grafana-iframe');
    const fallback = document.getElementById('grafana-fallback');
    if (iframe && fallback) {
        iframe.style.display = 'none';
        fallback.style.display = 'block';
    }
}

// Check if Grafana iframe loaded successfully
window.addEventListener('load', () => {
    const grafanaIframe = document.getElementById('grafana-iframe');
    if (grafanaIframe) {
        grafanaIframe.addEventListener('load', () => {
            // Iframe loaded successfully
            console.log('Grafana iframe loaded');
        });
        
        grafanaIframe.addEventListener('error', () => {
            handleGrafanaError();
        });
        
        // Check after a delay if iframe is blocked
        setTimeout(() => {
            try {
                // Try to access iframe content (will fail if X-Frame-Options blocks it)
                const iframeDoc = grafanaIframe.contentDocument || grafanaIframe.contentWindow.document;
            } catch (e) {
                // Iframe is blocked, show fallback
                handleGrafanaError();
            }
        }, 2000);
    }
});

// Utility functions
function formatNumber(num) {
    if (num === null || num === undefined) return '-';
    return new Intl.NumberFormat().format(num);
}

function formatTimestamp(timestamp) {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleString();
}

// Allow Enter key in Prometheus query input
document.getElementById('prometheus-query')?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        executePrometheusQuery();
    }
});

