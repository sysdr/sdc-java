const API_BASE = '';

// Initialize dashboard
document.addEventListener('DOMContentLoaded', () => {
    refreshHealth();
    loadStats();
    setInterval(refreshHealth, 30000); // Refresh every 30 seconds
    setInterval(loadStats, 5000); // Refresh stats every 5 seconds for real-time updates
});

// Health Check Functions
async function refreshHealth() {
    try {
        const response = await fetch(`${API_BASE}/api/health`);
        const healthData = await response.json();
        
        const healthGrid = document.getElementById('health-status');
        healthGrid.innerHTML = '';
        
        for (const [serviceName, health] of Object.entries(healthData)) {
            const healthItem = document.createElement('div');
            healthItem.className = `health-item ${health.success ? 'up' : 'down'}`;
            
            const name = document.createElement('div');
            name.className = 'health-item-name';
            name.textContent = serviceName;
            
            const status = document.createElement('div');
            status.className = 'health-item-status';
            status.textContent = health.success 
                ? `Status: ${health.status} - UP` 
                : `Status: ${health.status} - DOWN - ${health.error}`;
            
            healthItem.appendChild(name);
            healthItem.appendChild(status);
            healthGrid.appendChild(healthItem);
        }
    } catch (error) {
        console.error('Error fetching health:', error);
    }
}

// Statistics Functions
async function loadStats() {
    try {
        const response = await fetch(`${API_BASE}/api/stats`);
        const stats = await response.json();
        
        const statsDisplay = document.getElementById('stats-display');
        statsDisplay.innerHTML = '';
        
        // Prometheus metrics (real-time) - always show, even if 0
        if (stats.metrics) {
            const writesCard = createStatCard('Total Writes', formatNumber(stats.metrics.totalWrites || 0));
            statsDisplay.appendChild(writesCard);
            
            const readsCard = createStatCard('Total Reads', formatNumber(stats.metrics.totalReads || 0));
            statsDisplay.appendChild(readsCard);
            
            const pendingHintsCard = createStatCard('Pending Hints', stats.metrics.pendingHints || 0);
            statsDisplay.appendChild(pendingHintsCard);
            
            if (stats.metrics.merkleComparisons > 0 || stats.metrics.merkleBuilds > 0) {
                const merkleCard = createStatCard('Merkle Operations', formatNumber((stats.metrics.merkleComparisons || 0) + (stats.metrics.merkleBuilds || 0)));
                statsDisplay.appendChild(merkleCard);
            }
            
            if (stats.metrics.readRepairs > 0) {
                const repairsCard = createStatCard('Read Repairs', formatNumber(stats.metrics.readRepairs || 0));
                statsDisplay.appendChild(repairsCard);
            }
            
            if (stats.metrics.reconciliationJobs > 0) {
                const reconCard = createStatCard('Reconciliation Jobs', formatNumber(stats.metrics.reconciliationJobs || 0));
                statsDisplay.appendChild(reconCard);
            }
            
            if (stats.metrics.hintsStored > 0) {
                const hintsStoredCard = createStatCard('Hints Stored', formatNumber(stats.metrics.hintsStored || 0));
                statsDisplay.appendChild(hintsStoredCard);
            }
            
            if (stats.metrics.hintsDelivered > 0) {
                const hintsDeliveredCard = createStatCard('Hints Delivered', formatNumber(stats.metrics.hintsDelivered || 0));
                statsDisplay.appendChild(hintsDeliveredCard);
            }
        }
        
        // Hint stats (from API)
        if (stats.hints) {
            if (stats.metrics && stats.metrics.pendingHints === undefined) {
                const hintCard = createStatCard('Pending Hints', stats.hints.pending || 0);
                statsDisplay.appendChild(hintCard);
            }
            
            if (stats.hints.delivered !== undefined) {
                const deliveredCard = createStatCard('Delivered Hints', formatNumber(stats.hints.delivered || 0));
                statsDisplay.appendChild(deliveredCard);
            }
            
            if (stats.hints.expired !== undefined) {
                const expiredCard = createStatCard('Expired Hints', formatNumber(stats.hints.expired || 0));
                statsDisplay.appendChild(expiredCard);
            }
        }
        
        // Job stats
        if (stats.jobs) {
            if (stats.jobs.total !== undefined) {
                const totalJobsCard = createStatCard('Total Jobs', stats.jobs.total);
                statsDisplay.appendChild(totalJobsCard);
            }
            
            if (stats.jobs.pending !== undefined) {
                const pendingJobsCard = createStatCard('Pending Jobs', stats.jobs.pending);
                statsDisplay.appendChild(pendingJobsCard);
            }
            
            if (stats.jobs.running !== undefined) {
                const runningJobsCard = createStatCard('Running Jobs', stats.jobs.running);
                statsDisplay.appendChild(runningJobsCard);
            }
            
            if (stats.jobs.completed !== undefined) {
                const completedJobsCard = createStatCard('Completed Jobs', formatNumber(stats.jobs.completed));
                statsDisplay.appendChild(completedJobsCard);
            }
            
            if (stats.jobs.failed !== undefined) {
                const failedJobsCard = createStatCard('Failed Jobs', stats.jobs.failed);
                statsDisplay.appendChild(failedJobsCard);
            }
        }
    } catch (error) {
        console.error('Error loading stats:', error);
        const statsDisplay = document.getElementById('stats-display');
        statsDisplay.innerHTML = '<div class="stat-card" style="background: #e74c3c;"><h3>Error</h3><div class="value">Failed to load stats</div></div>';
    }
}

function formatNumber(num) {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
}

function createStatCard(title, value) {
    const card = document.createElement('div');
    card.className = 'stat-card';
    card.innerHTML = `
        <h3>${title}</h3>
        <div class="value">${value}</div>
    `;
    return card;
}

// Tab Functions
function showTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Remove active class from all buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected tab
    document.getElementById(`${tabName}-tab`).classList.add('active');
    
    // Add active class to clicked button
    event.target.classList.add('active');
}

function showMonitoringTab(tabName) {
    // Hide all monitoring tabs
    document.querySelectorAll('.monitoring-tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Remove active class from all buttons
    document.querySelectorAll('.monitoring-tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected tab
    document.getElementById(`${tabName}-tab`).classList.add('active');
    
    // Add active class to clicked button
    event.target.classList.add('active');
}

// Write Operation
async function executeWrite(event) {
    event.preventDefault();
    const resultBox = document.getElementById('write-result');
    resultBox.textContent = 'Executing...';
    resultBox.className = 'result-box';
    
    try {
        const response = await fetch(`${API_BASE}/api/proxy/write`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                partitionId: document.getElementById('write-partition').value,
                message: document.getElementById('write-message').value
            })
        });
        
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Read Operation
async function executeRead(event) {
    event.preventDefault();
    const resultBox = document.getElementById('read-result');
    resultBox.textContent = 'Executing...';
    resultBox.className = 'result-box';
    
    try {
        const partitionId = document.getElementById('read-partition').value;
        const version = document.getElementById('read-version').value;
        
        const response = await fetch(`${API_BASE}/api/proxy/read/${partitionId}/${version}`);
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Merkle Tree Build
async function executeMerkleBuild(event) {
    event.preventDefault();
    const resultBox = document.getElementById('merkle-build-result');
    resultBox.textContent = 'Executing...';
    resultBox.className = 'result-box';
    
    try {
        const response = await fetch(`${API_BASE}/api/proxy/merkle/build`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                partitionId: document.getElementById('merkle-build-partition').value,
                nodeUrl: document.getElementById('merkle-build-node').value
            })
        });
        
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Merkle Tree Compare
async function executeMerkleCompare(event) {
    event.preventDefault();
    const resultBox = document.getElementById('merkle-compare-result');
    resultBox.textContent = 'Executing...';
    resultBox.className = 'result-box';
    
    try {
        const response = await fetch(`${API_BASE}/api/proxy/merkle/compare`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                partitionId: document.getElementById('merkle-compare-partition').value,
                node1Url: document.getElementById('merkle-compare-node1').value,
                node2Url: document.getElementById('merkle-compare-node2').value
            })
        });
        
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Coordinator Functions
async function loadJobs() {
    const resultBox = document.getElementById('jobs-result');
    resultBox.textContent = 'Loading...';
    resultBox.className = 'result-box';
    
    try {
        const status = document.getElementById('job-status-filter').value;
        const url = status 
            ? `${API_BASE}/api/proxy/coordinator/jobs?status=${status}`
            : `${API_BASE}/api/proxy/coordinator/jobs`;
        
        const response = await fetch(url);
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

async function triggerReconciliation() {
    const resultBox = document.getElementById('jobs-result');
    resultBox.textContent = 'Triggering...';
    resultBox.className = 'result-box';
    
    try {
        const response = await fetch(`${API_BASE}/api/proxy/coordinator/trigger`, {
            method: 'POST'
        });
        
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
        
        // Reload jobs after triggering
        setTimeout(loadJobs, 2000);
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Hint Manager Functions
async function loadHintStats() {
    const resultBox = document.getElementById('hints-result');
    resultBox.textContent = 'Loading...';
    resultBox.className = 'result-box';
    
    try {
        const response = await fetch(`${API_BASE}/api/proxy/hints/stats`);
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

async function loadPendingHints() {
    const resultBox = document.getElementById('hints-result');
    resultBox.textContent = 'Loading...';
    resultBox.className = 'result-box';
    
    try {
        const response = await fetch(`${API_BASE}/api/proxy/hints/pending`);
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Storage Node Functions
async function executeStorageRead(event) {
    event.preventDefault();
    const resultBox = document.getElementById('storage-result');
    resultBox.textContent = 'Executing...';
    resultBox.className = 'result-box';
    
    try {
        const node = document.getElementById('storage-node').value;
        const partitionId = document.getElementById('storage-partition').value;
        const version = document.getElementById('storage-version').value;
        
        let url;
        if (version) {
            url = `${API_BASE}/api/proxy/storage/${node}/read/${partitionId}/${version}`;
        } else {
            url = `${API_BASE}/api/proxy/storage/${node}/read/${partitionId}/latest`;
        }
        
        const response = await fetch(url);
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = `result-box ${result.success ? 'success' : 'error'}`;
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

// Prometheus Query Functions
async function executePrometheusQuery(event) {
    event.preventDefault();
    const resultBox = document.getElementById('prometheus-query-result');
    resultBox.textContent = 'Executing...';
    resultBox.className = 'result-box';
    
    try {
        const query = document.getElementById('prometheus-query').value;
        const response = await fetch(`${API_BASE}/api/prometheus/query?q=${encodeURIComponent(query)}`);
        const result = await response.json();
        resultBox.textContent = JSON.stringify(result, null, 2);
        resultBox.className = 'result-box success';
    } catch (error) {
        resultBox.textContent = `Error: ${error.message}`;
        resultBox.className = 'result-box error';
    }
}

function runCommonQuery(query) {
    document.getElementById('prometheus-query').value = query;
    document.getElementById('prometheus-query-form').dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
}

