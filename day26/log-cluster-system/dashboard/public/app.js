const API_BASE = '/api';

// Tab switching
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const tabName = btn.dataset.tab;
        switchTab(tabName);
    });
});

function switchTab(tabName) {
    // Update buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
    
    // Update content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    document.getElementById(tabName).classList.add('active');
    
    // Auto-refresh overview when switching to it
    if (tabName === 'overview') {
        refreshAll();
    }
}

// API call helper
async function apiCall(endpoint, options = {}) {
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        });
        const data = await response.json();
        return data;
    } catch (error) {
        return { success: false, error: error.message };
    }
}

// Format JSON output
function formatJSON(obj) {
    return JSON.stringify(obj, null, 2);
}

// Cluster Operations
async function checkClusterStatus() {
    const output = document.getElementById('cluster-status-output');
    output.textContent = 'Loading...';
    
    const result = await apiCall('/cluster/status');
    output.textContent = formatJSON(result);
    
    // Update overview card
    if (result.success && result.data) {
        updateClusterStatusCard(result.data);
    }
}

function updateClusterStatusCard(data) {
    const card = document.getElementById('cluster-status-card');
    card.innerHTML = `
        <div class="status-item">
            <span class="status-label">Total Nodes:</span>
            <span class="status-value">${data.totalNodes || 0}</span>
        </div>
        <div class="status-item">
            <span class="status-label">Healthy Nodes:</span>
            <span class="status-value healthy">${data.healthyNodes || 0}</span>
        </div>
        <div class="status-item">
            <span class="status-label">Has Quorum:</span>
            <span class="status-value ${data.hasQuorum ? 'healthy' : 'unhealthy'}">${data.hasQuorum ? 'Yes' : 'No'}</span>
        </div>
    `;
}

async function getMembership() {
    const output = document.getElementById('membership-output');
    output.textContent = 'Loading...';
    
    const result = await apiCall('/cluster/membership');
    output.textContent = formatJSON(result);
}

async function sendGossip() {
    const input = document.getElementById('gossip-input');
    const output = document.getElementById('gossip-output');
    
    let data;
    try {
        data = JSON.parse(input.value);
    } catch (e) {
        output.textContent = `Error: Invalid JSON - ${e.message}`;
        return;
    }
    
    output.textContent = 'Loading...';
    const result = await apiCall('/cluster/gossip', {
        method: 'POST',
        body: JSON.stringify(data)
    });
    output.textContent = formatJSON(result);
}

async function antiEntropy() {
    const input = document.getElementById('anti-entropy-input');
    const output = document.getElementById('anti-entropy-output');
    
    let data;
    try {
        data = JSON.parse(input.value);
    } catch (e) {
        output.textContent = `Error: Invalid JSON - ${e.message}`;
        return;
    }
    
    output.textContent = 'Loading...';
    const result = await apiCall('/cluster/anti-entropy', {
        method: 'POST',
        body: JSON.stringify(data)
    });
    output.textContent = formatJSON(result);
}

// Health Checks
async function checkHealth(service) {
    const output = document.getElementById('health-output');
    output.textContent = `Loading health for ${service}...`;
    
    const result = await apiCall(`/health/${service}`);
    output.textContent = formatJSON(result);
}

async function checkAllHealth() {
    const output = document.getElementById('all-health-output');
    output.textContent = 'Loading...';
    
    const result = await apiCall('/health/all');
    
    if (result.success && result.data) {
        let html = '';
        for (const [service, healthResult] of Object.entries(result.data)) {
            const status = healthResult.success && healthResult.data?.status === 'UP' ? 'up' : 'down';
            const statusText = healthResult.success && healthResult.data?.status === 'UP' ? 'UP' : 'DOWN';
            html += `
                <div class="health-detail-item">
                    <h4>${service.toUpperCase()}</h4>
                    <div>Status: <strong class="${status}">${statusText}</strong></div>
                    <pre style="margin-top: 10px; font-size: 0.85em;">${formatJSON(healthResult)}</pre>
                </div>
            `;
        }
        output.innerHTML = html;
    } else {
        output.textContent = formatJSON(result);
    }
    
    // Update overview
    updateHealthOverview(result);
}

function updateHealthOverview(result) {
    const container = document.getElementById('health-overview');
    
    if (!result.success || !result.data) {
        container.innerHTML = '<div class="loading">Failed to load health data</div>';
        return;
    }
    
    let html = '';
    for (const [service, healthResult] of Object.entries(result.data)) {
        const status = healthResult.success && healthResult.data?.status === 'UP' ? 'up' : 'down';
        const statusText = healthResult.success && healthResult.data?.status === 'UP' ? 'UP' : 'DOWN';
        html += `
            <div class="health-item ${status}">
                <div>${service.toUpperCase()}</div>
                <div style="font-size: 0.9em; margin-top: 5px;">${statusText}</div>
            </div>
        `;
    }
    container.innerHTML = html;
}

// Prometheus Operations
async function executePromQuery() {
    const query = document.getElementById('prom-query').value;
    const output = document.getElementById('prom-query-output');
    
    if (!query) {
        output.textContent = 'Error: Please enter a query';
        return;
    }
    
    output.textContent = 'Loading...';
    const result = await apiCall(`/prometheus/query?q=${encodeURIComponent(query)}`);
    output.textContent = formatJSON(result);
}

function setQueryRangeDefaults() {
    const now = Math.floor(Date.now() / 1000);
    const oneHourAgo = now - 3600;
    
    document.getElementById('prom-start').value = oneHourAgo;
    document.getElementById('prom-end').value = now;
    document.getElementById('prom-step').value = '15s';
}

async function executePromQueryRange() {
    const query = document.getElementById('prom-query-range').value;
    let start = document.getElementById('prom-start').value.trim();
    let end = document.getElementById('prom-end').value.trim();
    const step = document.getElementById('prom-step').value.trim();
    const output = document.getElementById('prom-range-output');
    
    if (!query) {
        output.textContent = 'Error: Please enter a query';
        return;
    }
    
    // If start/end are empty, use defaults (last hour)
    if (!start || !end) {
        const now = Math.floor(Date.now() / 1000);
        const oneHourAgo = now - 3600;
        start = start || oneHourAgo.toString();
        end = end || now.toString();
        document.getElementById('prom-start').value = start;
        document.getElementById('prom-end').value = end;
    }
    
    // Validate that start and end are numeric
    if (isNaN(parseInt(start)) || isNaN(parseInt(end))) {
        output.textContent = 'Error: Start and End must be valid Unix timestamps (numbers). Click "Use Default (Last Hour)" to auto-fill.';
        return;
    }
    
    let url = `/prometheus/query-range?q=${encodeURIComponent(query)}`;
    if (start) url += `&start=${start}`;
    if (end) url += `&end=${end}`;
    if (step) url += `&step=${step}`;
    
    output.textContent = 'Loading...';
    const result = await apiCall(url);
    output.textContent = formatJSON(result);
}

async function getPromTargets() {
    const output = document.getElementById('prom-targets-output');
    output.textContent = 'Loading...';
    
    const result = await apiCall('/prometheus/targets');
    output.textContent = formatJSON(result);
}

function setQuery(query) {
    document.getElementById('prom-query').value = query;
    executePromQuery();
}

// Metrics
async function getMetrics(service) {
    const output = document.getElementById('metrics-output');
    output.textContent = `Loading metrics for ${service}...`;
    
    const result = await apiCall(`/metrics/${service}`);
    
    if (result.success && result.data) {
        // Format Prometheus metrics nicely
        output.textContent = result.data;
    } else {
        output.textContent = formatJSON(result);
    }
}

// Refresh all
async function refreshAll() {
    await Promise.all([
        checkClusterStatus(),
        checkAllHealth()
    ]);
}

// Auto-refresh overview every 30 seconds
setInterval(() => {
    if (document.getElementById('overview').classList.contains('active')) {
        refreshAll();
    }
}, 30000);

// Initial load
refreshAll();

