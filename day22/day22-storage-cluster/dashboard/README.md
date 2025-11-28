# Storage Cluster Dashboard

A comprehensive web dashboard for monitoring and operating the distributed storage cluster.

## Features

- **Real-time System Status**: Monitor health of all cluster components
- **Cluster Topology**: Visualize cluster structure and node distribution
- **Operations Panel**: Perform write and read operations directly from the dashboard
- **Prometheus Integration**: Query metrics and visualize data with interactive charts
- **Auto-refresh**: Automatic updates every 5 seconds

## Access

The dashboard is available at: **http://localhost:8888**

## Usage

### System Status
View real-time health status of:
- Cluster Coordinator
- Storage Nodes (1, 2, 3)
- Write Gateway
- Read Gateway

### Cluster Operations

**Write Operation:**
1. Enter a key (e.g., "user-123")
2. Enter content to store
3. Click "Write" to perform the operation

**Read Operation:**
1. Enter a key to retrieve
2. Click "Read" to fetch the data

### Metrics & Monitoring

**Prometheus Query:**
- Enter any Prometheus query in the query field
- Click "Query" to execute
- View results in JSON format

**Quick Metrics:**
Click any quick metric button to:
- Load the metric query
- Display results
- Update relevant charts

**Charts:**
- Replication Metrics: Success and failure rates
- Gateway Operations: Write and read throughput
- Latency Metrics: Replication latency over time

## API Endpoints

The dashboard server provides the following API endpoints:

- `GET /api/topology` - Get cluster topology
- `GET /api/health` - Get health status of all services
- `POST /api/write` - Perform write operation
- `GET /api/read/:key` - Perform read operation
- `GET /api/prometheus/query` - Query Prometheus metrics
- `GET /api/prometheus/query_range` - Query Prometheus metrics with time range

## Development

To run the dashboard locally:

```bash
cd dashboard
npm install
npm start
```

The dashboard will be available at http://localhost:8888

