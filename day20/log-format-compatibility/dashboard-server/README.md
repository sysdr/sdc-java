# Dashboard Server

Web-based dashboard for the Log Format Compatibility Layer system.

## Features

- **System Architecture Overview**: Visual representation of all services and their status
- **Real-Time Metrics**: Live charts showing message ingestion rates, normalization rates, error rates, and memory usage
- **Prometheus Integration**: Direct query interface and embedded Prometheus UI
- **Grafana Integration**: Embedded Grafana dashboards for advanced visualization
- **Log Query Interface**: Search and filter logs by level, source, and hostname
- **Service Health Monitoring**: Real-time status of all services
- **Kafka Topics Display**: View all Kafka topics in the system

## Prerequisites

- Node.js (v14 or higher)
- npm

## Installation

```bash
cd dashboard-server
npm install
```

## Running

```bash
npm start
# Or
./start.sh
```

The dashboard will be available at: http://localhost:8085

## API Endpoints

The dashboard server provides the following API endpoints:

- `GET /api/health` - Health status of all services
- `GET /api/stats` - System statistics
- `GET /api/logs/search` - Query logs with filters
- `GET /api/prometheus/query` - Execute Prometheus queries
- `GET /api/prometheus/query_range` - Get time series data
- `GET /api/kafka/topics` - List Kafka topics

## WebSocket

The dashboard uses WebSocket for real-time updates:
- Health status updates every 5 seconds
- Statistics updates every 5 seconds

## Configuration

The server connects to the following services (configured in `server.js`):

- Syslog Adapter: http://localhost:8081
- Journald Adapter: http://localhost:8082
- Format Normalizer: http://localhost:8083
- API Gateway: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

## Development

For development with auto-reload:

```bash
npm run dev
```

(Requires nodemon: `npm install -g nodemon`)

