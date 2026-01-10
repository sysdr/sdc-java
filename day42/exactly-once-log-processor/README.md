# Exactly-Once Log Processing System

Production-grade distributed log processing system demonstrating exactly-once processing semantics with Kafka, Redis, and PostgreSQL.

## Architecture

### Components

- **API Gateway**: Entry point for log ingestion (Port 8080)
- **Log Producer**: Idempotent Kafka producer with transactional writes (Port 8081)
- **Log Consumer**: Transactional consumer with deduplication (Port 8082)
- **Kafka**: Message broker with transaction support (Ports 9092/9093)
- **Redis**: Distributed idempotency cache (Port 6379)
- **PostgreSQL**: Persistent storage for processed logs (Port 5432)
- **Prometheus**: Metrics collection (Port 9090)
- **Grafana**: Metrics visualization (Port 3000)

### Exactly-Once Guarantees

1. **Idempotent Producer**
   - Automatic sequence numbering prevents duplicate writes
   - Configured with `enable.idempotence=true`, `acks=all`

2. **Transactional Messaging**
   - Atomic multi-partition writes with two-phase commit
   - Consumer reads only committed transactions (`isolation.level=read_committed`)

3. **Distributed Deduplication**
   - Redis-based idempotency keys with 24-hour TTL
   - Database-level uniqueness constraint on event IDs

4. **Transactional Offset Management**
   - Offsets committed within Kafka transactions
   - Manual acknowledgment after successful processing

5. **State Reconciliation**
   - Scheduled comparison of Kafka offsets vs. database records
   - Alerts on divergence exceeding threshold

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 17+
- Maven 3.8+
- curl (for testing)

### Setup

```bash
# Run the setup script
./setup.sh
```

This will:
1. Start infrastructure (Kafka, Redis, PostgreSQL, Prometheus, Grafana)
2. Create Kafka topics with proper configuration
3. Build Maven projects
4. Start application services

### Verify System

```bash
# Check service health
curl http://localhost:8080/api/health  # Gateway
curl http://localhost:8081/api/logs/health  # Producer
curl http://localhost:8082/actuator/health  # Consumer

# Send a test log event
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{
    "eventType": "INFO",
    "service": "test-service",
    "message": "Test log message",
    "severity": "LOW",
    "userId": "user123"
  }'
```

## Testing

### Integration Tests

```bash
./integration-tests/test-exactly-once.sh
```

Tests:
- Duplicate event detection
- Atomic batch sends
- Redis idempotency cache
- Prometheus metrics

### Load Testing

```bash
./load-test.sh
```

Generates sustained load for 60 seconds with concurrent requests to verify:
- Throughput under load
- Duplicate detection rate
- Transaction abort rate
- Processing latency

## Monitoring

### Prometheus

Access metrics at `http://localhost:9090`

Key metrics:
- `log_producer_send_success_total`: Successful sends
- `log_producer_transaction_abort_total`: Aborted transactions
- `log_consumer_processed_total`: Successfully processed events
- `log_consumer_duplicates_total`: Duplicate events detected
- `idempotency_cache_hit_total`: Redis cache hits

### Grafana

Access dashboards at `http://localhost:3000` (admin/admin)

Pre-configured datasource: Prometheus

Sample queries:
```promql
# Processing rate
rate(log_consumer_processed_total[5m])

# Duplicate detection rate
rate(log_consumer_duplicates_total[5m]) / rate(log_consumer_processed_total[5m])

# Transaction abort rate
rate(log_producer_transaction_abort_total[5m])
```

### Database Inspection

```bash
# Connect to PostgreSQL
docker exec -it exactly-once-log-processor-postgres-1 psql -U postgres -d logprocessor

# Check processed logs
\dt
SELECT COUNT(*) FROM processed_logs;
SELECT * FROM processed_logs ORDER BY processed_at DESC LIMIT 10;

# Check for duplicates
SELECT event_id, COUNT(*) 
FROM processed_logs 
GROUP BY event_id 
HAVING COUNT(*) > 1;
```

### Redis Inspection

```bash
# Connect to Redis
docker exec -it exactly-once-log-processor-redis-1 redis-cli

# Check idempotency keys
KEYS idempotency:*
TTL idempotency:some-event-id:1234567890

# Get cache statistics
INFO stats
```

## Failure Scenarios

### Test Producer Failure

```bash
# Stop producer mid-transaction
docker-compose stop log-producer
# Send events (should fail)
curl -X POST http://localhost:8080/api/logs -d '{...}'
# Restart producer
docker-compose start log-producer
# Events should resume processing
```

### Test Consumer Crash

```bash
# Stop consumer
docker-compose stop log-consumer
# Send events (will accumulate in Kafka)
# Restart consumer
docker-compose start log-consumer
# Consumer should resume from last committed offset
# No duplicates should be processed
```

### Test Redis Failure

```bash
# Stop Redis
docker-compose stop redis
# Send events (depends on fail-open/fail-closed configuration)
# Restart Redis
docker-compose start redis
# Deduplication should resume
```

## Performance Tuning

### Producer Configuration

```yaml
# High throughput
batch.size: 32768
linger.ms: 20
compression.type: snappy

# Low latency
batch.size: 8192
linger.ms: 0
acks: 1  # Trade durability for speed
```

### Consumer Configuration

```yaml
# High throughput
max.poll.records: 500
fetch.min.bytes: 10240

# Low latency
max.poll.records: 50
fetch.max.wait.ms: 100
```

### Redis Tuning

```bash
# Reduce memory usage
# Use shorter TTLs for idempotency keys
# Implement probabilistic deduplication with Bloom filters
```

## Capacity Planning

### Expected Performance

- **Throughput**: 40,000-50,000 events/sec with exactly-once enabled
- **Latency**: p99 < 50ms for end-to-end processing
- **Memory**: ~500MB per service under normal load
- **Storage**: ~1KB per processed event

### Scaling Guidelines

**Horizontal Scaling**:
- Add more consumer instances (up to partition count)
- Increase Kafka partitions for higher parallelism
- Shard Redis cache by event ID hash

**Vertical Scaling**:
- Increase JVM heap: `-Xmx2g -Xms2g`
- Tune database connection pool size
- Increase Kafka broker resources

## Troubleshooting

### High Duplicate Rate

```bash
# Check producer idempotence configuration
curl http://localhost:8081/actuator/configprops | grep idempotence

# Verify Redis connectivity
docker exec log-consumer redis-cli -h redis PING
```

### Transaction Timeouts

```bash
# Check transaction coordinator logs
docker logs exactly-once-log-processor-kafka-1 | grep transaction

# Increase timeout
# Add to producer config: transaction.timeout.ms: 120000
```

### State Divergence

```bash
# Trigger manual reconciliation
curl -X POST http://localhost:8082/actuator/scheduledtasks

# Check reconciliation metrics
curl http://localhost:8082/actuator/prometheus | grep divergence
```

## Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (deletes all data)
docker-compose down -v

# Remove built artifacts
mvn clean
```

## Architecture Decisions

### Why Kafka Transactions?

Kafka transactions provide atomic multi-partition writes and coordinated offset commits, essential for exactly-once semantics. Alternative approaches like distributed locks or database-driven coordination add significant complexity.

### Why Redis for Idempotency?

Redis provides sub-millisecond latency for idempotency checks, critical for maintaining throughput. PostgreSQL-based deduplication would add ~10-20ms per event.

### Why Manual Offset Management?

Auto-commit can acknowledge messages before processing completes, causing data loss on crashes. Manual acknowledgment within transactions ensures atomicity.

## Next Steps

- Implement Kafka Streams for real-time aggregations
- Add schema registry for message evolution
- Implement multi-region replication
- Add data retention policies
- Implement consumer group rebalancing strategies

## References

- [Kafka Exactly-Once Semantics](https://kafka.apache.org/documentation/#semantics)
- [Spring Kafka Transactions](https://docs.spring.io/spring-kafka/reference/kafka/transactions.html)
- [Redis Best Practices](https://redis.io/docs/manual/patterns/distributed-locks/)
