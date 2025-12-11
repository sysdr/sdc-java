// Dashboard JavaScript

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    checkHealth();
    loadPrometheusDemo();
    loadGrafanaDemo();
});

// Health checks
async function checkHealth() {
    // Check coordinator
    try {
        const response = await fetch('/api/health/coordinator');
        const data = await response.json();
        const statusEl = document.getElementById('coordinator-health');
        statusEl.textContent = `Status: ${data.status.toUpperCase()}`;
        statusEl.className = `health-status ${data.status}`;
    } catch (error) {
        document.getElementById('coordinator-health').textContent = 'Status: ERROR';
        document.getElementById('coordinator-health').className = 'health-status error';
    }
    
    // Check partitions
    try {
        const response = await fetch('/api/health/partitions');
        const data = await response.json();
        const statusEl = document.getElementById('partitions-health');
        const healthyCount = data.partitions.filter(p => p.status === 'healthy').length;
        statusEl.textContent = `Status: ${healthyCount}/${data.partitions.length} Healthy`;
        statusEl.className = `health-status ${healthyCount === data.partitions.length ? 'healthy' : 'unhealthy'}`;
    } catch (error) {
        document.getElementById('partitions-health').textContent = 'Status: ERROR';
        document.getElementById('partitions-health').className = 'health-status error';
    }
}

// Load statistics
async function loadStats() {
    const statsContent = document.getElementById('stats-content');
    statsContent.innerHTML = '<div class="loading"></div> Loading...';
    
    try {
        const response = await fetch('/api/stats');
        const data = await response.json();
        
        statsContent.innerHTML = Object.entries(data).map(([key, value]) => `
            <div class="stat-card">
                <div class="stat-value">${value}</div>
                <div class="stat-label">${key.replace(/([A-Z])/g, ' $1').trim()}</div>
            </div>
        `).join('');
        
    } catch (error) {
        statsContent.innerHTML = `<div style="color: var(--error-color);">Error loading statistics: ${error.message}</div>`;
    }
}

// Load metadata
async function loadMetadata(partitionId) {
    const metadataContent = document.getElementById(`metadata-${partitionId}`);
    metadataContent.innerHTML = '<div class="loading"></div> Loading...';
    
    try {
        const response = await fetch(`/api/metadata/${partitionId}`);
        const data = await response.json();
        
        metadataContent.innerHTML = `<pre>${JSON.stringify(data, null, 2)}</pre>`;
        
    } catch (error) {
        metadataContent.innerHTML = `<div style="color: var(--error-color);">Error loading metadata: ${error.message}</div>`;
    }
}

// Prometheus chart instances
let prometheusChart = null;
let grafanaPanelChart = null;
let exampleQueryCharts = {};

// Load Prometheus demo
async function loadPrometheusDemo() {
    try {
        const response = await fetch('/api/prometheus/demo');
        const data = await response.json();
        
        // Populate example queries dropdown
        const exampleSelect = document.getElementById('example-queries');
        data.queries.forEach((query, index) => {
            const option = document.createElement('option');
            option.value = index;
            option.textContent = query.name;
            exampleSelect.appendChild(option);
        });
        
        // Store queries for later use
        window.prometheusQueries = data.queries;
        
        const queriesList = document.getElementById('prometheus-queries');
        queriesList.innerHTML = data.queries.map((query, index) => `
            <div class="query-item" id="query-item-${index}">
                <h4>${query.name}</h4>
                <div class="query-code">${query.query}</div>
                <div class="description">${query.description}</div>
                <div class="query-actions-inline">
                    <button onclick="executeExampleQuery('${query.query.replace(/'/g, "\\'")}', ${index})" class="btn btn-primary btn-small">Execute Query</button>
                    <button onclick="executeExampleRangeQuery('${query.query.replace(/'/g, "\\'")}', ${index})" class="btn btn-secondary btn-small">Execute Range (Graph)</button>
                    <button onclick="loadQueryIntoInput('${query.query.replace(/'/g, "\\'")}')" class="btn btn-small">Use in Form</button>
                </div>
                <div id="query-results-${index}" class="query-item-results" style="display: none;">
                    <h5>Results:</h5>
                    <div id="query-metrics-${index}" class="query-metrics"></div>
                    <div id="query-chart-${index}" style="margin-top: 1rem;">
                        <canvas id="query-chart-canvas-${index}"></canvas>
                    </div>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading Prometheus demo:', error);
    }
}

// Load query into input
function loadQueryIntoInput(query) {
    document.getElementById('prometheus-query-input').value = query;
}

// Load example query
function loadExampleQuery(index) {
    if (index && window.prometheusQueries) {
        const query = window.prometheusQueries[parseInt(index)];
        if (query) {
            document.getElementById('prometheus-query-input').value = query.query;
        }
    }
}

// Execute Prometheus instant query
async function executePrometheusQuery() {
    const query = document.getElementById('prometheus-query-input').value.trim();
    if (!query) {
        alert('Please enter a query');
        return;
    }
    
    const resultsContainer = document.getElementById('prometheus-results');
    const metricsDisplay = document.getElementById('prometheus-metrics');
    const chartContainer = document.getElementById('prometheus-chart-container');
    
    resultsContainer.style.display = 'block';
    metricsDisplay.innerHTML = '<div class="loading"></div> Loading...';
    chartContainer.style.display = 'none';
    
    try {
        const response = await fetch('/api/prometheus/query', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ query: query })
        });
        
        const data = await response.json();
        
        if (data.error) {
            metricsDisplay.innerHTML = `<div style="color: var(--error-color);">Error: ${data.error}</div>`;
            return;
        }
        
        if (data.data && data.data.result) {
            const results = data.data.result;
            
            // Display metrics
            if (results.length === 0) {
                metricsDisplay.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">No data found for this query.</div>';
            } else {
                metricsDisplay.innerHTML = `
                    <div class="metrics-table">
                        <table>
                            <thead>
                                <tr>
                                    <th>Metric</th>
                                    <th>Value</th>
                                    <th>Labels</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${results.map(r => `
                                    <tr>
                                        <td>${r.metric.__name__ || 'N/A'}</td>
                                        <td><strong>${Array.isArray(r.value) ? r.value[1] : r.value}</strong></td>
                                        <td>${JSON.stringify(r.metric).replace(/"/g, '').replace(/,/g, ', ')}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                `;
            }
        } else {
            metricsDisplay.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">Unexpected response format.</div>';
        }
        
    } catch (error) {
        metricsDisplay.innerHTML = `<div style="color: var(--error-color);">Error: ${error.message}</div>`;
    }
}

// Execute Prometheus range query (for graphs)
async function executePrometheusRangeQuery() {
    const query = document.getElementById('prometheus-query-input').value.trim();
    if (!query) {
        alert('Please enter a query');
        return;
    }
    
    const resultsContainer = document.getElementById('prometheus-results');
    const metricsDisplay = document.getElementById('prometheus-metrics');
    const chartContainer = document.getElementById('prometheus-chart-container');
    
    resultsContainer.style.display = 'block';
    metricsDisplay.innerHTML = '<div class="loading"></div> Loading time series data...';
    chartContainer.style.display = 'block';
    
    try {
        const response = await fetch('/api/prometheus/query_range', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                query: query,
                step: '15s'
            })
        });
        
        const data = await response.json();
        
        if (data.error) {
            metricsDisplay.innerHTML = `<div style="color: var(--error-color);">Error: ${data.error}</div>`;
            chartContainer.style.display = 'none';
            return;
        }
        
        if (data.data && data.data.result) {
            const results = data.data.result;
            
            if (results.length === 0) {
                metricsDisplay.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">No time series data found for this query.</div>';
                chartContainer.style.display = 'none';
                return;
            }
            
            // Prepare chart data
            const datasets = results.map((series, index) => {
                const color = `hsl(${(index * 360) / results.length}, 70%, 50%)`;
                const label = series.metric.__name__ || 
                    Object.entries(series.metric)
                        .filter(([k]) => k !== '__name__')
                        .map(([k, v]) => `${k}=${v}`)
                        .join(', ') || `Series ${index + 1}`;
                
                return {
                    label: label,
                    data: series.values.map(v => ({
                        x: new Date(v[0] * 1000),
                        y: parseFloat(v[1])
                    })),
                    borderColor: color,
                    backgroundColor: color + '20',
                    tension: 0.1,
                    fill: false
                };
            });
            
            // Destroy existing chart
            if (prometheusChart) {
                prometheusChart.destroy();
            }
            
            // Create new chart
            const ctx = document.getElementById('prometheus-chart').getContext('2d');
            prometheusChart = new Chart(ctx, {
                type: 'line',
                data: { datasets: datasets },
                options: {
                    responsive: true,
                    interaction: {
                        intersect: false,
                        mode: 'index'
                    },
                    scales: {
                        x: {
                            type: 'time',
                            time: {
                                displayFormats: {
                                    minute: 'HH:mm',
                                    hour: 'HH:mm'
                                }
                            },
                            title: {
                                display: true,
                                text: 'Time'
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
                        title: {
                            display: true,
                            text: 'Prometheus Query Results Over Time'
                        },
                        legend: {
                            display: true
                        }
                    }
                }
            });
            
            metricsDisplay.innerHTML = `
                <div style="padding: 1rem; background: var(--bg-color); border-radius: 6px; margin-bottom: 1rem;">
                    <strong>Query:</strong> ${query}<br>
                    <strong>Data Points:</strong> ${results.reduce((sum, r) => sum + r.values.length, 0)}<br>
                    <strong>Series:</strong> ${results.length}
                </div>
            `;
            
        } else {
            metricsDisplay.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">Unexpected response format.</div>';
            chartContainer.style.display = 'none';
        }
        
    } catch (error) {
        metricsDisplay.innerHTML = `<div style="color: var(--error-color);">Error: ${error.message}</div>`;
        chartContainer.style.display = 'none';
    }
}

// Load Grafana demo
async function loadGrafanaDemo() {
    try {
        const response = await fetch('/api/grafana/demo');
        const data = await response.json();
        
        // Store panels for later use
        window.grafanaPanels = data.panels;
        
        const panelsList = document.getElementById('grafana-panels');
        panelsList.innerHTML = data.panels.map((panel, index) => `
            <div class="panel-item">
                <h4>${panel.title}</h4>
                <span class="panel-type">${panel.type}</span>
                <div class="description">${panel.description}</div>
                <div class="queries">
                    ${panel.queries.map(query => `
                        <div class="query-code">${query}</div>
                    `).join('')}
                </div>
                <button onclick="executeGrafanaPanel(${index})" class="btn btn-primary" style="margin-top: 1rem;">View Demo</button>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading Grafana demo:', error);
    }
}

// Execute Grafana panel demo
async function executeGrafanaPanel(panelIndex) {
    if (!window.grafanaPanels || !window.grafanaPanels[panelIndex]) {
        alert('Panel not found');
        return;
    }
    
    const panel = window.grafanaPanels[panelIndex];
    const resultsContainer = document.getElementById('grafana-panel-results');
    const titleEl = document.getElementById('panel-results-title');
    const metricsDisplay = document.getElementById('grafana-panel-metrics');
    const chartContainer = document.getElementById('grafana-panel-chart-container');
    
    resultsContainer.style.display = 'block';
    titleEl.textContent = panel.title + ' - Demo Visualization';
    metricsDisplay.innerHTML = '<div class="loading"></div> Loading panel data...';
    chartContainer.style.display = 'block';
    
    try {
        const response = await fetch('/api/grafana/demo/panel', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                queries: panel.queries,
                type: panel.type
            })
        });
        
        const data = await response.json();
        
        if (data.error) {
            metricsDisplay.innerHTML = `<div style="color: var(--error-color);">Error: ${data.error}</div>`;
            chartContainer.style.display = 'none';
            return;
        }
        
        if (data.results && data.results.length > 0) {
            // Check for errors
            const errors = data.results.filter(r => r.error);
            if (errors.length > 0) {
                metricsDisplay.innerHTML = `
                    <div style="color: var(--warning-color); padding: 1rem;">
                        Some queries failed: ${errors.map(e => e.error).join(', ')}
                    </div>
                `;
            }
            
            // Get successful results
            const successfulResults = data.results.filter(r => r.data && !r.error);
            
            if (successfulResults.length === 0) {
                metricsDisplay.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">No data available for this panel.</div>';
                chartContainer.style.display = 'none';
                return;
            }
            
            // Prepare chart data based on panel type
            let datasets = [];
            let chartType = 'line';
            
            if (panel.type === 'Bar Chart') {
                chartType = 'bar';
            }
            
            successfulResults.forEach((result, resultIndex) => {
                if (result.data && result.data.data && result.data.data.result) {
                    result.data.data.result.forEach((series, seriesIndex) => {
                        const color = `hsl(${((resultIndex * 3 + seriesIndex) * 360) / (successfulResults.length * 3)}, 70%, 50%)`;
                        const label = series.metric.__name__ || 
                            Object.entries(series.metric)
                                .filter(([k]) => k !== '__name__')
                                .map(([k, v]) => `${k}=${v}`)
                                .join(', ') || `Series ${seriesIndex + 1}`;
                        
                        datasets.push({
                            label: label,
                            data: series.values.map(v => ({
                                x: new Date(v[0] * 1000),
                                y: parseFloat(v[1])
                            })),
                            borderColor: color,
                            backgroundColor: color + (chartType === 'bar' ? '80' : '20'),
                            tension: 0.1,
                            fill: chartType === 'line' && panel.type === 'Time Series'
                        });
                    });
                }
            });
            
            // Destroy existing chart
            if (grafanaPanelChart) {
                grafanaPanelChart.destroy();
            }
            
            // Create new chart
            const ctx = document.getElementById('grafana-panel-chart').getContext('2d');
            grafanaPanelChart = new Chart(ctx, {
                type: chartType,
                data: { datasets: datasets },
                options: {
                    responsive: true,
                    interaction: {
                        intersect: false,
                        mode: 'index'
                    },
                    scales: {
                        x: {
                            type: 'time',
                            time: {
                                displayFormats: {
                                    minute: 'HH:mm',
                                    hour: 'HH:mm'
                                }
                            },
                            title: {
                                display: true,
                                text: 'Time'
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
                        title: {
                            display: true,
                            text: panel.title
                        },
                        legend: {
                            display: true
                        }
                    }
                }
            });
            
            metricsDisplay.innerHTML = `
                <div style="padding: 1rem; background: var(--bg-color); border-radius: 6px; margin-bottom: 1rem;">
                    <strong>Panel Type:</strong> ${panel.type}<br>
                    <strong>Queries Executed:</strong> ${successfulResults.length}/${data.results.length}<br>
                    <strong>Data Series:</strong> ${datasets.length}
                </div>
            `;
            
            // Scroll to results
            resultsContainer.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            
        } else {
            metricsDisplay.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">No data available.</div>';
            chartContainer.style.display = 'none';
        }
        
    } catch (error) {
        metricsDisplay.innerHTML = `<div style="color: var(--error-color);">Error: ${error.message}</div>`;
        chartContainer.style.display = 'none';
    }
}

// Execute example query (instant)
async function executeExampleQuery(query, index) {
    const resultsDiv = document.getElementById(`query-results-${index}`);
    const metricsDiv = document.getElementById(`query-metrics-${index}`);
    const chartDiv = document.getElementById(`query-chart-${index}`);
    
    resultsDiv.style.display = 'block';
    metricsDiv.innerHTML = '<div class="loading"></div> Executing query...';
    chartDiv.style.display = 'none';
    
    try {
        const response = await fetch('/api/prometheus/query', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ query: query })
        });
        
        const data = await response.json();
        
        if (data.error) {
            metricsDiv.innerHTML = `<div style="color: var(--error-color);">Error: ${data.error}</div>`;
            return;
        }
        
        if (data.data && data.data.result) {
            const results = data.data.result;
            
            if (results.length === 0) {
                metricsDiv.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">No data found for this query.</div>';
            } else {
                metricsDiv.innerHTML = `
                    <div class="metrics-table">
                        <table>
                            <thead>
                                <tr>
                                    <th>Metric</th>
                                    <th>Value</th>
                                    <th>Labels</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${results.map(r => `
                                    <tr>
                                        <td>${r.metric.__name__ || 'N/A'}</td>
                                        <td><strong>${Array.isArray(r.value) ? r.value[1] : r.value}</strong></td>
                                        <td>${JSON.stringify(r.metric).replace(/"/g, '').replace(/,/g, ', ')}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                `;
            }
        } else {
            metricsDiv.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">Unexpected response format.</div>';
        }
        
        // Scroll to results
        resultsDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        
    } catch (error) {
        metricsDiv.innerHTML = `<div style="color: var(--error-color);">Error: ${error.message}</div>`;
    }
}

// Execute example range query (graph)
async function executeExampleRangeQuery(query, index) {
    const resultsDiv = document.getElementById(`query-results-${index}`);
    const metricsDiv = document.getElementById(`query-metrics-${index}`);
    const chartDiv = document.getElementById(`query-chart-${index}`);
    
    resultsDiv.style.display = 'block';
    metricsDiv.innerHTML = '<div class="loading"></div> Loading time series data...';
    chartDiv.style.display = 'block';
    
    try {
        const response = await fetch('/api/prometheus/query_range', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                query: query,
                step: '15s'
            })
        });
        
        const data = await response.json();
        
        if (data.error) {
            metricsDiv.innerHTML = `<div style="color: var(--error-color);">Error: ${data.error}</div>`;
            chartDiv.style.display = 'none';
            return;
        }
        
        if (data.data && data.data.result) {
            const results = data.data.result;
            
            if (results.length === 0) {
                metricsDiv.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">No time series data found for this query.</div>';
                chartDiv.style.display = 'none';
                return;
            }
            
            // Prepare chart data
            const datasets = results.map((series, seriesIndex) => {
                const color = `hsl(${(seriesIndex * 360) / results.length}, 70%, 50%)`;
                const label = series.metric.__name__ || 
                    Object.entries(series.metric)
                        .filter(([k]) => k !== '__name__')
                        .map(([k, v]) => `${k}=${v}`)
                        .join(', ') || `Series ${seriesIndex + 1}`;
                
                return {
                    label: label,
                    data: series.values.map(v => ({
                        x: new Date(v[0] * 1000),
                        y: parseFloat(v[1])
                    })),
                    borderColor: color,
                    backgroundColor: color + '20',
                    tension: 0.1,
                    fill: false
                };
            });
            
            // Destroy existing chart
            if (exampleQueryCharts[index]) {
                exampleQueryCharts[index].destroy();
            }
            
            // Create new chart
            const ctx = document.getElementById(`query-chart-canvas-${index}`).getContext('2d');
            exampleQueryCharts[index] = new Chart(ctx, {
                type: 'line',
                data: { datasets: datasets },
                options: {
                    responsive: true,
                    interaction: {
                        intersect: false,
                        mode: 'index'
                    },
                    scales: {
                        x: {
                            type: 'time',
                            time: {
                                displayFormats: {
                                    minute: 'HH:mm',
                                    hour: 'HH:mm'
                                }
                            },
                            title: {
                                display: true,
                                text: 'Time'
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
                        title: {
                            display: true,
                            text: 'Query Results Over Time'
                        },
                        legend: {
                            display: true
                        }
                    }
                }
            });
            
            metricsDiv.innerHTML = `
                <div style="padding: 1rem; background: var(--bg-color); border-radius: 6px; margin-bottom: 1rem;">
                    <strong>Query:</strong> ${query}<br>
                    <strong>Data Points:</strong> ${results.reduce((sum, r) => sum + r.values.length, 0)}<br>
                    <strong>Series:</strong> ${results.length}
                </div>
            `;
            
            // Scroll to results
            resultsDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            
        } else {
            metricsDiv.innerHTML = '<div style="padding: 1rem; color: var(--text-secondary);">Unexpected response format.</div>';
            chartDiv.style.display = 'none';
        }
        
    } catch (error) {
        metricsDiv.innerHTML = `<div style="color: var(--error-color);">Error: ${error.message}</div>`;
        chartDiv.style.display = 'none';
    }
}

// Auto-refresh health every 30 seconds
setInterval(checkHealth, 30000);

