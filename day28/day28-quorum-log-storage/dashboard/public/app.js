const API_BASE = window.location.origin;
let queryChart = null;
let operationHistory = [];

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    setupTabs();
    checkServices();
    setInterval(checkServices, 10000); // Check every 10 seconds
    loadPredefinedQueries();
});

// Tab switching
function setupTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const tabName = button.dataset.tab;
            
            // Remove active class from all
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabContents.forEach(content => content.classList.remove('active'));
            
            // Add active class to clicked
            button.classList.add('active');
            document.getElementById(tabName).classList.add('active');
        });
    });
}

// Service health check
async function checkServices() {
    try {
        const response = await fetch(`${API_BASE}/api/health`);
        const data = await response.json();
        
        updateServiceStatus('apiGatewayStatus', data.services.apiGateway);
        updateServiceStatus('prometheusStatus', data.services.prometheus);
        updateServiceStatus('grafanaStatus', data.services.grafana);
        
        const allOnline = data.services.apiGateway && data.services.prometheus && data.services.grafana;
        const statusDot = document.querySelector('.status-dot');
        const statusText = document.getElementById('statusText');
        
        if (allOnline) {
            statusDot.classList.add('online');
            statusText.textContent = 'All services online';
        } else {
            statusDot.classList.remove('online');
            statusText.textContent = 'Some services offline';
        }
    } catch (error) {
        console.error('Health check failed:', error);
        updateServiceStatus('apiGatewayStatus', false);
        updateServiceStatus('prometheusStatus', false);
        updateServiceStatus('grafanaStatus', false);
    }
}

function updateServiceStatus(elementId, isOnline) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = isOnline ? 'Online' : 'Offline';
        element.className = `service-status-indicator ${isOnline ? 'online' : 'offline'}`;
    }
}

// Write operation
async function performWrite() {
    const key = document.getElementById('writeKey').value;
    const value = document.getElementById('writeValue').value;
    const consistency = document.getElementById('writeConsistency').value;
    const resultBox = document.getElementById('writeResult');
    
    if (!key || !value) {
        resultBox.textContent = 'Error: Key and value are required';
        resultBox.className = 'result-box error';
        return;
    }
    
    resultBox.textContent = 'Writing...';
    resultBox.className = 'result-box';
    
    try {
        const startTime = Date.now();
        const response = await fetch(`${API_BASE}/api/operation/write`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key, value, consistency })
        });
        
        const data = await response.json();
        const latency = Date.now() - startTime;
        
        if (data.success) {
            resultBox.textContent = JSON.stringify({
                success: true,
                message: data.data.message,
                consistency: data.data.consistency,
                acknowledgedReplicas: data.data.acknowledgedReplicas,
                requiredReplicas: data.data.requiredReplicas,
                latency: `${latency}ms`,
                timestamp: data.timestamp
            }, null, 2);
            resultBox.className = 'result-box success';
            
            addToHistory('WRITE', { key, value, consistency, latency, success: true });
        } else {
            const errorObj = {
                success: false,
                error: data.error || 'Unknown error',
                latency: `${latency}ms`,
                timestamp: data.timestamp
            };
            if (data.details) {
                errorObj.details = data.details;
            }
            resultBox.textContent = JSON.stringify(errorObj, null, 2);
            resultBox.className = 'result-box error';
            
            addToHistory('WRITE', { key, value, consistency, latency, success: false });
        }
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Read operation
async function performRead() {
    const key = document.getElementById('readKey').value;
    const consistency = document.getElementById('readConsistency').value;
    const resultBox = document.getElementById('readResult');
    
    if (!key) {
        resultBox.textContent = 'Error: Key is required';
        resultBox.className = 'result-box error';
        return;
    }
    
    resultBox.textContent = 'Reading...';
    resultBox.className = 'result-box';
    
    try {
        const startTime = Date.now();
        const response = await fetch(`${API_BASE}/api/operation/read/${key}?consistency=${consistency}`);
        const data = await response.json();
        const latency = Date.now() - startTime;
        
        if (data.success) {
            resultBox.textContent = JSON.stringify({
                success: true,
                key: data.data.key,
                value: data.data.value,
                consistency: data.data.consistency,
                latency: `${latency}ms`,
                timestamp: data.timestamp
            }, null, 2);
            resultBox.className = 'result-box success';
            
            addToHistory('READ', { key, consistency, latency, success: true });
        } else {
            const errorObj = {
                success: false,
                error: data.error || 'Key not found',
                latency: `${latency}ms`,
                timestamp: data.timestamp
            };
            if (data.details) {
                errorObj.details = data.details;
            }
            resultBox.textContent = JSON.stringify(errorObj, null, 2);
            resultBox.className = 'result-box error';
            
            addToHistory('READ', { key, consistency, latency, success: false });
        }
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Operation history
function addToHistory(operation, details) {
    operationHistory.unshift({
        operation,
        ...details,
        timestamp: new Date().toISOString()
    });
    
    if (operationHistory.length > 20) {
        operationHistory = operationHistory.slice(0, 20);
    }
    
    updateHistoryDisplay();
}

function updateHistoryDisplay() {
    const historyList = document.getElementById('operationHistory');
    if (!historyList) return;
    
    if (operationHistory.length === 0) {
        historyList.innerHTML = '<p style="color: var(--text-secondary);">No operations yet</p>';
        return;
    }
    
    historyList.innerHTML = operationHistory.map(item => {
        const status = item.success ? '✓' : '✗';
        const statusColor = item.success ? 'var(--success)' : 'var(--error)';
        return `
            <div class="history-item">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div>
                        <strong style="color: ${statusColor};">${status} ${item.operation}</strong>
                        <span style="color: var(--text-secondary); margin-left: 10px;">${item.key || 'N/A'}</span>
                    </div>
                    <div style="text-align: right; font-size: 0.9em; color: var(--text-secondary);">
                        <div>${item.latency}ms</div>
                        <div>${new Date(item.timestamp).toLocaleTimeString()}</div>
                    </div>
                </div>
                ${item.consistency ? `<div style="margin-top: 5px; font-size: 0.85em; color: var(--text-secondary);">Consistency: ${item.consistency}</div>` : ''}
            </div>
        `;
    }).join('');
}

// Prometheus queries
let predefinedQueries = {};

async function loadPredefinedQueries() {
    try {
        const response = await fetch(`${API_BASE}/api/prometheus/queries`);
        const data = await response.json();
        predefinedQueries = data.queries;
    } catch (error) {
        console.error('Failed to load queries:', error);
    }
}

function loadQuery() {
    const selector = document.getElementById('querySelector');
    const customQuery = document.getElementById('customQuery');
    
    if (selector.value && predefinedQueries[selector.value]) {
        customQuery.value = predefinedQueries[selector.value];
    }
}

function loadExampleQuery(button) {
    const codeElement = button.previousElementSibling;
    const query = codeElement.textContent.trim();
    document.getElementById('customQuery').value = query;
}

async function executeQuery() {
    const query = document.getElementById('customQuery').value.trim();
    const resultBox = document.getElementById('queryResult');
    
    if (!query) {
        resultBox.textContent = 'Error: Please enter a query';
        resultBox.className = 'result-box error';
        return;
    }
    
    resultBox.textContent = 'Executing query...';
    resultBox.className = 'result-box';
    
    try {
        const response = await fetch(`${API_BASE}/api/prometheus/query`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query })
        });
        
        const data = await response.json();
        
        if (data.success) {
            const result = data.data.data;
            if (result.resultType === 'vector') {
                resultBox.textContent = JSON.stringify(result.result, null, 2);
            } else if (result.resultType === 'matrix') {
                resultBox.textContent = JSON.stringify(result.result, null, 2);
            } else {
                resultBox.textContent = JSON.stringify(result, null, 2);
            }
            resultBox.className = 'result-box success';
        } else {
            resultBox.textContent = `Error: ${data.error}`;
            resultBox.className = 'result-box error';
        }
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

async function executeQueryRange() {
    const query = document.getElementById('customQuery').value.trim();
    const resultBox = document.getElementById('queryResult');
    
    if (!query) {
        resultBox.textContent = 'Error: Please enter a query';
        resultBox.className = 'result-box error';
        return;
    }
    
    resultBox.textContent = 'Executing query range...';
    resultBox.className = 'result-box';
    
    try {
        const end = Math.floor(Date.now() / 1000);
        const start = end - 3600; // Last hour
        
        const response = await fetch(`${API_BASE}/api/prometheus/query_range`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query, start, end, step: '15s' })
        });
        
        const data = await response.json();
        
        if (data.success) {
            const result = data.data.data;
            resultBox.textContent = `Query executed successfully. Showing graph...`;
            resultBox.className = 'result-box success';
            
            if (result.resultType === 'matrix' && result.result.length > 0) {
                renderChart(result.result);
            } else {
                resultBox.textContent = 'No data points to display';
            }
        } else {
            resultBox.textContent = `Error: ${data.error}`;
            resultBox.className = 'result-box error';
        }
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

function renderChart(data) {
    const canvas = document.getElementById('queryChart');
    
    // Destroy existing chart if it exists (check both our variable and Chart.js registry)
    if (queryChart) {
        try {
            queryChart.destroy();
        } catch (e) {
            console.warn('Error destroying chart from variable:', e);
        }
        queryChart = null;
    }
    
    // Also check Chart.js registry for any existing chart on this canvas
    const existingChart = Chart.getChart(canvas);
    if (existingChart) {
        try {
            existingChart.destroy();
        } catch (e) {
            console.warn('Error destroying chart from registry:', e);
        }
    }
    
    // Get fresh context after destroying the chart
    const ctx = canvas.getContext('2d');
    
    const datasets = data.map((series, index) => {
        const colors = [
            'rgb(45, 134, 89)',
            'rgb(5, 150, 105)',
            'rgb(217, 119, 6)',
            'rgb(245, 158, 11)',
            'rgb(16, 185, 129)'
        ];
        
        return {
            label: series.metric.instance || series.metric.job || series.metric.name || `Series ${index + 1}`,
            data: series.values.map(([timestamp, value]) => ({
                x: timestamp * 1000,
                y: parseFloat(value)
            })),
            borderColor: colors[index % colors.length],
            backgroundColor: colors[index % colors.length] + '20',
            tension: 0.1,
            fill: false
        };
    });
    
    queryChart = new Chart(ctx, {
        type: 'line',
        data: { datasets },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            scales: {
                x: {
                    type: 'linear',
                    position: 'bottom',
                    title: {
                        display: true,
                        text: 'Time'
                    },
                    ticks: {
                        callback: function(value) {
                            const date = new Date(value);
                            return date.toLocaleTimeString();
                        },
                        maxTicksLimit: 10
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: 'Value'
                    }
                }
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            }
        }
    });
}

