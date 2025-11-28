# Partitioned Log Storage Dashboard

Professional web dashboard for monitoring and querying the Partitioned Log Storage System.

## Features

- **Real-time Metrics**: Live graphs showing logs produced rate, logs written rate, and query duration
- **Partition Statistics**: View partition counts, sizes, and distribution
- **Log Query Interface**: Query logs by time range, source, and log level
- **System Health**: Monitor all services (Producer, Consumer, Partition Manager, Query Service)
- **Operations Overview**: Understand system architecture and component status

## Installation

```bash
cd dashboard-server
npm install
```

## Running

```bash
npm start
```

The dashboard will be available at: http://localhost:3001

## Configuration

The dashboard connects to:
- **Prometheus**: http://localhost:9090 (default)
- **Query Service**: http://localhost:8084 (default)
- **Producer Service**: http://localhost:8081 (default)
- **PostgreSQL**: localhost:5432 (default)

You can override these by setting environment variables:

```bash
PROMETHEUS_URL=http://localhost:9090 \
QUERY_SERVICE_URL=http://localhost:8084 \
PRODUCER_SERVICE_URL=http://localhost:8081 \
DB_HOST=localhost \
DB_PORT=5432 \
DB_NAME=logdb \
DB_USER=loguser \
DB_PASSWORD=logpass \
npm start
```

## Usage

1. **View Metrics**: The dashboard automatically refreshes metrics every 15 seconds
2. **Query Logs**: Use the query interface to search logs by:
   - Time range (start and end time)
   - Source service (optional)
   - Log level (ERROR, WARN, INFO, DEBUG)
   - Result limit
3. **Monitor Partitions**: View partition statistics and sizes
4. **Check System Health**: Monitor all service statuses in real-time

## API Endpoints

The dashboard server exposes the following API endpoints:

- `GET /api/health` - System health status
- `GET /api/metrics/dashboard` - Dashboard metrics
- `GET /api/metrics/query?query=<promql>` - Prometheus query
- `GET /api/metrics/query_range?query=<promql>&start=<ts>&end=<ts>&step=<interval>` - Prometheus range query
- `GET /api/partitions/stats` - Partition statistics
- `POST /api/query/logs` - Query logs (proxies to Query Service)

