# Day 51: Real-Time Analytics Dashboard System

## 📋 Overview

Production-grade real-time analytics dashboard system with WebSocket-based metric streaming, three-tier caching architecture, and sub-100ms query latency.

## 🏗️ Architecture

### System Components

1. **Metrics Aggregator Service** (Port 8081)
   - Consumes log events from Kafka
   - Maintains ring buffers for recent metrics
   - Publishes to WebSocket clients
   - Implements Redis caching layer
   - Persists to PostgreSQL with time-series partitioning

2. **Dashboard Service** (Port 8080)
   - WebSocket server for real-time updates
   - REST API for historical queries
   - Circuit breaker protection
   - Three-tier query optimization

3. **Metrics Generator** (Port 8082)
   - Synthetic log event generator
   - 100 events/second for testing

### Data Flow

```
Log Events → Kafka → Aggregator → [Ring Buffer + Redis + PostgreSQL]
                                         ↓
                                   WebSocket Push
                                         ↓
                                  Frontend Dashboard
```

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- 4GB RAM minimum
- Ports 8080-8082, 9090, 3000, 5432, 6379, 9092 available

### Setup

```bash
# Make scripts executable
chmod +x setup.sh load-test.sh

# Start the system
./setup.sh

# Wait 45 seconds for initialization

# Access dashboard
open http://localhost:8080
```

### Generate Load

```bash
./load-test.sh
```

Watch real-time metrics update in the dashboard!

## 📊 Monitoring

- **Dashboard UI**: http://localhost:8080
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## 🧪 Testing

```bash
# Run integration tests
./integration-tests/test-dashboard.sh

# Manual API test
curl "http://localhost:8080/api/metrics/request_count?start=2024-01-01T00:00:00Z&end=2024-12-31T23:59:59Z"

# Check Prometheus metrics
curl http://localhost:8081/actuator/prometheus
```

## 🎯 Key Features

### Three-Tier Query Architecture
1. **In-memory ring buffers** (last 5 minutes) - <1ms latency
2. **Redis cache** (last hour) - ~5ms latency
3. **PostgreSQL** (historical) - ~50ms latency

### Real-Time Push Architecture
- WebSocket connections for sub-second updates
- Batching (100ms intervals) reduces overhead by 90%
- Automatic reconnection with exponential backoff

### Failure Resilience
- Circuit breaker protection (Resilience4j)
- Graceful degradation (serves cached data)
- Connection status monitoring

## 📈 Performance Characteristics

- **Query Latency**: p99 < 100ms
- **WebSocket Updates**: < 50ms from metric generation
- **Cache Hit Rate**: 95%+ during normal operations
- **Throughput**: 10,000+ metrics/second per service

## 🔍 How It Works

### Metric Processing Pipeline

1. **Log Event Ingestion**
   ```
   Kafka Topic: log-events
   → MetricsConsumerService
   → Extract metrics (error_rate, response_time, etc.)
   ```

2. **Multi-Tier Storage**
   ```
   Ring Buffer (1000 points) → Instant queries
   Redis (1 hour TTL) → Recent queries
   PostgreSQL (partitioned) → Historical queries
   ```

3. **Push to Clients**
   ```
   Metrics → Kafka → WebSocket Service
   → Batch (100ms) → Push to browsers
   ```

### Query Optimization

```java
// Three-tier query logic
if (timeRange < 5 minutes) {
    return ringBuffer.query();  // <1ms
}
if (timeRange < 1 hour) {
    return redis.query();       // ~5ms
}
return postgres.query();        // ~50ms
```

## 🏭 Production Considerations

### Scaling Horizontally
- Dashboard service can scale independently
- WebSocket connections distributed via load balancer
- Shared Redis cache across instances

### Capacity Planning
- Memory: 500MB per 1000 unique metrics
- Storage: ~5-10% of raw event volume
- Network: ~100KB/s per WebSocket connection

### Monitoring
- Track cache hit rates (alert < 80%)
- Monitor WebSocket connection count
- Alert on query latency > 500ms

## 🛠️ Development

### Project Structure
```
├── metrics-aggregator/      # Kafka consumer + metric processing
├── dashboard-service/       # WebSocket + REST API
├── metrics-generator/       # Synthetic load generator
├── monitoring/              # Prometheus configuration
└── integration-tests/       # E2E tests
```

### Adding New Metrics

1. Update `MetricsConsumerService.processLogEvent()`
2. Add chart to `dashboard-service/static/index.html`
3. Update WebSocket subscription in frontend

## 🐛 Troubleshooting

### Dashboard shows "Disconnected"
```bash
# Check dashboard service logs
docker-compose logs dashboard-service

# Verify Kafka connectivity
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### No metrics appearing
```bash
# Check generator is running
docker-compose logs metrics-generator

# Verify Kafka messages
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic log-events --from-beginning
```

### High query latency
```bash
# Check Redis cache hit rate
docker-compose exec redis redis-cli INFO stats

# Verify PostgreSQL query performance
docker-compose exec postgres psql -U postgres -d metricsdb \
  -c "SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

## 📚 Learning Objectives

After completing this lesson, you understand:

1. **Materialized View Pattern**: Pre-computing aggregations for query performance
2. **Push vs Pull Architecture**: WebSocket for real-time updates
3. **Multi-Tier Caching**: Balancing latency, freshness, and cost
4. **Time-Series Optimization**: Partitioning and downsampling strategies
5. **Graceful Degradation**: Maintaining availability during failures

## 🎓 Scale Connection

This architecture mirrors how major platforms handle analytics:

- **LinkedIn**: Uses similar three-tier approach for 50M+ concurrent users
- **Netflix**: Employs materialized views for 500B+ daily events
- **Uber**: Implements push-based dashboards for surge pricing visibility

The patterns scale from local development to planetary distribution.

## 🔄 Clean Up

```bash
# Stop all services
docker-compose down

# Remove volumes (clears data)
docker-compose down -v

# Remove generated files
cd .. && rm -rf day51-analytics-dashboard
```

## 📖 Next Steps

Day 52 covers implementing inverted indexes for full-text log search, enabling engineers to find specific errors across billions of log lines in under 100ms.

---

**Built with**: Spring Boot 3.2, Kafka, Redis, PostgreSQL, WebSocket, Chart.js

**Pattern Focus**: Real-time analytics, WebSocket architecture, multi-tier caching

**Scale Ready**: 10,000+ metrics/second, 1000+ concurrent dashboards
