# Dashboard Quick Start

## Installation

```bash
cd dashboard-server
npm install
```

## Start Server

```bash
npm start
```

Dashboard will be available at: **http://localhost:8085**

## Features Overview

### 1. System Architecture
- View all services and their status
- Real-time health monitoring
- Service descriptions and ports

### 2. System Statistics
- Total logs processed
- Syslog messages count
- Journald messages count
- Normalized events count

### 3. Real-Time Metrics
- Message ingestion rate charts
- Normalization rate charts
- Error rate monitoring
- Memory usage tracking

### 4. Monitoring Dashboards
- **Prometheus Tab**: Query interface and embedded Prometheus UI
- **Grafana Tab**: Embedded Grafana dashboards

### 5. Log Query Interface
- Filter by level (DEBUG, INFO, WARN, ERROR, FATAL)
- Filter by source (syslog, journald)
- Filter by hostname
- Adjustable result limit

### 6. Kafka Topics
- View all Kafka topics in the system
- Auto-refreshes every 30 seconds

## WebSocket Real-Time Updates

The dashboard uses WebSocket for:
- Health status updates (every 5 seconds)
- Statistics updates (every 5 seconds)

## Troubleshooting

### Dashboard not loading
1. Ensure all services are running
2. Check that port 8085 is available
3. Verify Node.js is installed: `node --version`

### Metrics not showing
1. Ensure Prometheus is running: http://localhost:9090
2. Verify services are exposing metrics at `/actuator/prometheus`
3. Check browser console for errors

### Grafana/Prometheus iframes not loading
1. Ensure Prometheus is accessible: http://localhost:9090
2. Ensure Grafana is accessible: http://localhost:3000
3. Check browser console for CORS or connection errors

### Log queries returning empty
1. Verify API Gateway is running on port 8080
2. Check that logs have been ingested
3. Try sending test syslog messages

