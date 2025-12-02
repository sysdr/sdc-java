# Leader Election Cluster Dashboard

A professional web dashboard for monitoring and interacting with the Leader Election Storage Cluster.

## Features

- **Real-time Cluster Status**: Monitor leader election, node states, and cluster health
- **Interactive Operations**: Write data to the cluster directly from the dashboard
- **Prometheus Integration**: View metrics and execute PromQL queries
- **Grafana Links**: Quick access to advanced monitoring dashboards
- **Professional Design**: Clean, modern interface with green/orange/red color scheme

## Quick Start

1. **Start the cluster** (if not already running):
   ```bash
   ./setup.sh
   ```

2. **Start the dashboard server**:
   ```bash
   python3 dashboard/server.py
   ```

3. **Open in browser**:
   ```
   http://localhost:8000
   ```

## Dashboard Sections

### Cluster Status
- Real-time cluster health indicator
- Current leader identification
- Cluster size and node count
- Last update timestamp

### Cluster Nodes
- Visual representation of all nodes
- Node states (LEADER, FOLLOWER, CANDIDATE)
- Current term and port information
- Color-coded status indicators

### Operations
- **Write Data**: Send write requests to the cluster
- **Refresh Status**: Manually update cluster status
- Real-time response display

### Prometheus Metrics
- Key metrics display:
  - Total elections
  - Total writes
  - Heartbeat success rate
- Interactive PromQL query examples:
  - Election rate
  - Write throughput
  - Heartbeat success ratio
- Direct links to Prometheus UI

### Grafana Integration
- Quick access to Grafana dashboards
- Pre-configured dashboard information
- Links to visualization tools

## API Endpoints Used

The dashboard interacts with:
- `http://localhost:8080/api/status` - Cluster status
- `http://localhost:8080/api/write` - Write operations
- `http://localhost:8081/raft/status` - Node 1 status
- `http://localhost:8082/raft/status` - Node 2 status
- `http://localhost:8083/raft/status` - Node 3 status
- `http://localhost:9090/api/v1/query` - Prometheus queries

## Auto-Refresh

The dashboard automatically refreshes cluster status every 5 seconds. Use the refresh button (bottom-right) for manual refresh.

## Troubleshooting

### Dashboard not loading
- Ensure the cluster is running: `docker-compose ps`
- Check that API Gateway is accessible: `curl http://localhost:8080/api/status`

### CORS errors
- The dashboard server includes CORS headers
- If issues persist, ensure all services are running on localhost

### Metrics not showing
- Wait a few seconds after cluster startup for metrics to populate
- Check Prometheus is running: `curl http://localhost:9090/-/healthy`

## Browser Compatibility

- Chrome/Edge (recommended)
- Firefox
- Safari
- Modern browsers with ES6 support

## Color Scheme

The dashboard uses a professional color palette:
- **Green**: Healthy status, leader nodes, success indicators
- **Orange**: Followers, warnings, query highlights
- **Red**: Errors, candidates, failure states
- **Dark theme**: Professional dark background

No purple or blue colors are used as per design requirements.

