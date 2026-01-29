# Access Information - Real-Time Log Indexing System

## 🌐 Service URLs

### Application Services
- **Log Producer API**: http://localhost:8081
  - Health: http://localhost:8081/actuator/health
  - Metrics: http://localhost:8081/actuator/metrics
  - Prometheus: http://localhost:8081/actuator/prometheus
  - API Endpoint: http://localhost:8081/api/logs

- **Log Indexer API**: http://localhost:8082
  - Health: http://localhost:8082/actuator/health
  - Metrics: http://localhost:8082/actuator/metrics
  - Prometheus: http://localhost:8082/actuator/prometheus

- **Search API**: http://localhost:8083
  - Health: http://localhost:8083/actuator/health
  - Metrics: http://localhost:8083/actuator/metrics
  - Prometheus: http://localhost:8083/actuator/prometheus
  - Search Endpoint: http://localhost:8083/api/search

### Monitoring & Dashboards
- **Prometheus**: http://localhost:9090
  - No authentication required
  - Query endpoint: http://localhost:9090/api/v1/query
  - Targets: http://localhost:9090/targets

- **Grafana**: http://localhost:3000
  - **Username**: `admin`
  - **Password**: `admin`
  - First login will prompt to change password (optional)

### Infrastructure Services
- **PostgreSQL**: localhost:5432
  - Database: `logindexing`
  - Username: `postgres`
  - Password: `postgres`

- **Redis**: localhost:6379
  - No password (default configuration)

- **Kafka**: localhost:9092
  - Topic: `raw-logs`
  - No authentication (default configuration)

- **Zookeeper**: localhost:2181
  - No authentication (default configuration)

## 🔑 Credentials Summary

| Service | Username | Password | Notes |
|---------|----------|----------|-------|
| Grafana | admin | admin | Change on first login (optional) |
| PostgreSQL | postgres | postgres | Database: logindexing |
| Redis | - | - | No authentication |
| Kafka | - | - | No authentication |
| Prometheus | - | - | No authentication |
| Application APIs | - | - | No authentication |

## 📊 Key Prometheus Queries

Access Prometheus at http://localhost:9090 and use these queries:

```
# Total logs produced
logs_produced_total

# Total documents indexed
documents_indexed_total

# Total search queries
search_queries_total

# Log production rate (per second)
rate(logs_produced_total[1m])

# Indexing rate (per second)
rate(documents_indexed_total[1m])

# Indexing latency (95th percentile)
histogram_quantile(0.95, rate(indexing_latency_seconds_bucket[5m]))

# Search latency (95th percentile)
histogram_quantile(0.95, rate(search_latency_seconds_bucket[5m]))

# Cache hit rate
rate(search_cache_hits_total[1m]) / (rate(search_cache_hits_total[1m]) + rate(search_cache_misses_total[1m]))
```

## 🧪 API Examples

### Produce Logs
```bash
# Single log
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-123",
    "timestamp": "2026-01-28T10:00:00Z",
    "level": "INFO",
    "service": "test-service",
    "message": "Test log message"
  }'

# Batch logs
curl -X POST http://localhost:8081/api/logs/batch/100
```

### Search Logs
```bash
# Simple search
curl "http://localhost:8083/api/search?query=error&limit=10"

# Advanced search
curl -X POST http://localhost:8083/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "authentication",
    "level": "ERROR",
    "service": "auth-service",
    "limit": 50
  }'
```

## 🔍 Health Check Endpoints

All services provide health endpoints:
- http://localhost:8081/actuator/health
- http://localhost:8082/actuator/health
- http://localhost:8083/actuator/health

Expected response: `{"status":"UP"}`

## 📝 Notes

1. All services run on localhost - no external access required
2. Services are accessible from WSL/Windows host
3. Docker containers are accessible via `host.docker.internal` from within containers
4. First Grafana login may prompt for password change (can be skipped)
5. All application APIs are unauthenticated (development setup)
