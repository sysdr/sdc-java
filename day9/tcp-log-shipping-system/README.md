# TCP Log Shipping System - Day 9

A production-grade distributed log processing system demonstrating TCP-based log shipping with resilience patterns, buffering, and observability.

## Architecture Overview

This system consists of four main services:

1. **Log Producer** - Generates application logs and ships them via TCP
   - Implements buffered batching for network efficiency
   - Circuit breaker pattern for fault tolerance
   - Exponential backoff for reconnection
   - Exposes metrics at `:8081/actuator/prometheus`

2. **Log Receiver** - Accepts TCP connections and publishes to Kafka
   - Multi-threaded TCP server on port 9090
   - Bridges synchronous TCP to asynchronous Kafka
   - Metrics at `:8082/actuator/prometheus`

3. **Log Consumer** - Processes logs from Kafka
   - Persists logs to PostgreSQL
   - Caches ERROR logs in Redis
   - Metrics at `:8083/actuator/prometheus`

4. **API Gateway** - Entry point for external clients
   - Health checks and system information
   - Metrics at `:8080/actuator/prometheus`

### Infrastructure Components

- **Apache Kafka** - Message streaming platform
- **PostgreSQL** - Persistent log storage
- **Redis** - Cache for recent error logs
- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization

## Quick Start

### Prerequisites

- Docker and Docker Compose
- 8GB RAM minimum
- Ports 8080-8083, 3000, 5432, 6379, 9090, 9092 available

### Deployment

```bash
# Make setup script executable
chmod +x setup.sh

# Start the entire system
./setup.sh
```

The system will start in approximately 2-3 minutes.

### Verify Deployment

```bash
# Check all services are healthy
docker compose ps

# Run integration tests
./integration-tests/test-system.sh

# Check metrics in Prometheus
open http://localhost:9090

# View dashboards in Grafana
open http://localhost:3000  # Login: admin/admin
```

## Usage

### Send a Single Log

```bash
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "message": "Application started successfully",
    "service": "my-app"
  }'
```

### Send Batch Logs

```bash
curl -X POST http://localhost:8081/api/logs/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "level": "INFO",
      "message": "User logged in",
      "service": "auth-service"
    },
    {
      "level": "ERROR",
      "message": "Failed to connect to database",
      "service": "data-service"
    }
  ]'
```

## Load Testing

Run the included load test to generate sustained traffic:

```bash
./load-test.sh
```

This will:
- Send logs for 60 seconds
- Use 10 concurrent workers
- Generate ~100+ requests/second
- Display real-time metrics

### Monitoring Load Test Results

1. **Grafana Dashboard**: http://localhost:3000
   - View "TCP Log Shipping System" dashboard
   - Watch metrics: logs/sec, buffer depth, failures

2. **Prometheus Queries**: http://localhost:9090
   ```promql
   # Logs sent per second
   rate(logs_sent_total[1m])
   
   # Current buffer depth
   buffer_depth
   
   # Connection failure rate
   rate(connection_failures_total[5m])
   ```

3. **Application Metrics**:
   ```bash
   curl http://localhost:8081/actuator/prometheus | grep logs_
   ```

## Key Metrics

### Producer Metrics
- `logs_sent_total` - Total logs successfully sent
- `logs_dropped_total` - Logs dropped due to full buffer
- `buffer_depth` - Current buffer size (0-10000)
- `connection_failures_total` - TCP connection failures

### Receiver Metrics
- `logs_received_total` - Total logs received via TCP
- `kafka_publish_total` - Logs published to Kafka
- `tcp_connections_total` - Total TCP connections accepted

### Consumer Metrics
- `logs_processed_total` - Logs processed from Kafka
- `logs_persisted_total` - Logs saved to PostgreSQL
- `logs_cached_total` - ERROR logs cached in Redis

## System Design Patterns

### 1. Buffered Batching
- Reduces network overhead by ~94%
- Configurable batch size (default: 100 messages)
- Time-based flush every 100ms

### 2. Circuit Breaker
- Prevents cascade failures
- Opens after 50% failure rate
- Auto-recovery after 30 seconds

### 3. Exponential Backoff
- Graceful reconnection on failures
- Prevents thundering herd
- Max delay: 30 seconds

### 4. Decoupled Architecture
- Kafka as buffer between producer and consumer
- Services can fail independently
- Consumer can catch up after downtime

## Failure Scenarios

### Test Server Unavailability
```bash
# Stop receiver
docker compose stop log-receiver

# Send logs - they will be buffered
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{"level":"INFO","message":"Test","service":"test"}'

# Watch metrics - buffer_depth will increase
curl http://localhost:8081/actuator/prometheus | grep buffer_depth

# Restart receiver - logs will be delivered
docker compose start log-receiver
```

### Test Database Failure
```bash
# Stop PostgreSQL
docker compose stop postgres

# Logs will queue in Kafka
# Consumer will retry with backoff

# Restart database
docker compose start postgres

# Consumer will catch up automatically
```

## Troubleshooting

### Services won't start
```bash
# Check logs
docker compose logs -f <service-name>

# Restart specific service
docker compose restart <service-name>
```

### Logs not flowing
```bash
# Verify TCP connection
telnet localhost 9090

# Check Kafka topics
docker compose exec kafka kafka-topics --list \
  --bootstrap-server localhost:9092

# Verify database connection
docker compose exec postgres psql -U postgres -d logsdb -c "SELECT COUNT(*) FROM log_entries;"
```

### High buffer depth
- Indicates throughput mismatch
- Check receiver capacity
- Check Kafka performance
- Consider horizontal scaling

## Scaling Strategies

### Horizontal Scaling
```yaml
# Scale producer instances
docker compose up -d --scale log-producer=3

# Scale consumer instances
docker compose up -d --scale log-consumer=3
```

### Kafka Partitioning
- Increase Kafka partitions for parallelism
- One consumer per partition max
- Preserves ordering within partition

### Database Optimization
- Add indexes on timestamp, level, service
- Implement partitioning for large datasets
- Consider time-series database for high volume

## Development

### Build Locally
```bash
# Build all services
mvn clean install

# Build specific service
cd log-producer && mvn clean package
```

### Run Tests
```bash
# Unit tests
mvn test

# Integration tests
./integration-tests/test-system.sh
```

## Cleanup

```bash
# Stop all services
docker compose down

# Remove volumes (deletes data)
docker compose down -v

# Remove images
docker compose down --rmi all
```

## Next Steps (Day 10)

Tomorrow we'll add UDP support for:
- Lower latency (5ms vs 20-50ms TCP)
- Higher throughput
- Trade-off: no delivery guarantees
- Use cases: metrics, traces

## Architecture Insights

1. **Memory vs Disk Buffering**: We chose memory for speed, accepting 0.01% data loss on crashes
2. **Batch Size Trade-off**: 100ms flush interval balances latency and efficiency
3. **Circuit Breaker Philosophy**: We drop logs when the receiver is down to preserve application stability
4. **Kafka as Buffer**: Decouples producer and consumer lifecycles
5. **Observability First**: Every component exports metrics for debugging production issues

## Production Checklist

- [ ] Configure persistent volumes for Kafka
- [ ] Set up Kafka replication (factor 3)
- [ ] Implement authentication (TLS for TCP, SASL for Kafka)
- [ ] Configure log retention policies
- [ ] Set up alerts in Grafana
- [ ] Implement rate limiting
- [ ] Add distributed tracing (Zipkin/Jaeger)
- [ ] Document runbook for incidents
- [ ] Set up automated backups for PostgreSQL
- [ ] Load test at expected peak + 2x capacity

## Resources

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Apache Kafka](https://kafka.apache.org/documentation/)
- [Resilience4j](https://resilience4j.readme.io/docs/circuitbreaker)
- [Prometheus](https://prometheus.io/docs/introduction/overview/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)

## License

MIT License - Educational purposes for Day 9 of 254-Day System Design Course
