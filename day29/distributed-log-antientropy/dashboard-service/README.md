# Dashboard Service

A comprehensive web dashboard for monitoring and operating the Distributed Log Anti-Entropy System.

## Features

- **System Health Monitoring**: Real-time health status of all services
- **System Statistics**: Overview of hints, jobs, and system metrics
- **Interactive Operations**: Execute all system operations through a user-friendly interface
  - Write/Read log entries
  - Merkle tree operations (build, compare)
  - Anti-entropy coordinator operations
  - Hint manager operations
  - Direct storage node access
- **Prometheus Integration**: Query Prometheus metrics directly
- **Grafana Integration**: Embedded Grafana dashboards
- **Professional UI**: Clean, modern interface with responsive design

## Access

Once the service is running, access the dashboard at:
- **Dashboard**: http://localhost:8088

## API Endpoints

The dashboard service provides the following API endpoints:

- `GET /api/health` - Aggregated health check of all services
- `GET /api/stats` - System statistics (hints, jobs)
- `POST /api/proxy/write` - Proxy write operation
- `GET /api/proxy/read/:partitionId/:version` - Proxy read operation
- `POST /api/proxy/merkle/build` - Build Merkle tree
- `POST /api/proxy/merkle/compare` - Compare Merkle trees
- `GET /api/proxy/coordinator/jobs` - Get reconciliation jobs
- `POST /api/proxy/coordinator/trigger` - Trigger reconciliation
- `GET /api/proxy/hints/pending` - Get pending hints
- `GET /api/proxy/hints/stats` - Get hint statistics
- `GET /api/prometheus/query` - Query Prometheus metrics
- `GET /api/prometheus/query_range` - Query Prometheus metrics with time range

## Running Locally

```bash
cd dashboard-service
npm install
npm start
```

The dashboard will be available at http://localhost:8088

## Docker

The dashboard service is automatically included in the docker-compose setup. It will start with all other services when you run:

```bash
./setup.sh
```

