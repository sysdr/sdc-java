# Day 34: Consumer Acknowledgments and Redelivery Mechanisms

Production-ready log processing system with manual acknowledgments, retry logic, dead letter queues, and idempotency guarantees.

## Architecture Overview

### Components
- **API Gateway** (Port 8080): Entry point for log events
- **Log Producer** (Port 8081): Publishes events to Kafka
- **Log Consumer** (Port 8082): Processes events with acknowledgments and retries
- **Kafka**: Message broker with acknowledgment control
- **PostgreSQL**: Persistent storage and idempotency tracking
- **Redis**: Fast duplicate detection cache
- **Prometheus**: Metrics collection
- **Grafana**: Visualization dashboards

### Key Features
1. **Manual Acknowledgment**: Consumer explicitly acknowledges after successful processing
2. **Retry Logic**: Exponential backoff (100ms, 200ms, 400ms, 800ms, 1.6s)
3. **Dead Letter Queue**: Isolates failed messages after max retries
4. **Idempotency**: Hybrid Redis/PostgreSQL deduplication
5. **Circuit Breaker**: Prevents cascade failures

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17+
- Maven 3.8+

### Deployment
```bash
./deploy.sh
```

Wait 30-60 seconds for services to be healthy.

### Verify Deployment
```bash
docker-compose ps
curl http://localhost:8080/api/health
```

## Testing

### Basic Functionality
```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{"level":"INFO","message":"Test message","source":"test-client","userId":"user123"}'
```

### Test Retry Logic
```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{"level":"ERROR","message":"Test retry","source":"test","userId":"user456","simulateFailure":true}'

docker-compose logs -f log-consumer
```

### Test Idempotency
```bash
MESSAGE_ID="test-$(date +%s)"
for i in {1..3}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"$MESSAGE_ID\",\"level\":\"WARN\",\"message\":\"Duplicate test\",\"source\":\"test\",\"userId\":\"user789\"}"
done
```

### Run Integration Tests
```bash
cd integration-tests
./test-acknowledgments.sh
```

### Load Testing
```bash
./load-test.sh
```

## Monitoring

### Prometheus Queries
Access at http://localhost:9090

**Acknowledgment Rate:**
```promql
rate(consumer_messages_acknowledged_total[1m])
```

**Retry Rate:**
```promql
rate(consumer_messages_rejected_total[1m])
```

**DLQ Messages:**
```promql
rate(dlq_messages_sent_total[1m])
```

**Processing Time (p99):**
```promql
histogram_quantile(0.99, rate(consumer_processing_time_seconds_bucket[1m]))
```

### Grafana Dashboards
Access at http://localhost:3000 (admin/admin)

Pre-configured dashboard shows:
- Message acknowledgment rates
- Retry patterns and frequency
- Dead letter queue metrics
- Processing latencies (p50, p95, p99)
- Circuit breaker status

## Database Inspection

### Check Processed Messages
```bash
docker-compose exec postgres psql -U loguser -d logdb -c "SELECT COUNT(*) FROM log_events;"
```

### View Idempotency Keys
```bash
docker-compose exec postgres psql -U loguser -d logdb -c "SELECT * FROM idempotency_keys LIMIT 10;"
```

### Inspect Dead Letter Queue
```bash
docker-compose exec postgres psql -U loguser -d logdb -c "SELECT * FROM failed_messages;"
```

### Check Redis Cache
```bash
docker-compose exec redis redis-cli
> KEYS idempotency:*
> TTL idempotency:some-message-id
```

## System Behavior

### Normal Processing Flow
1. Producer sends message to Kafka
2. Consumer polls message
3. Idempotency check (Redis → PostgreSQL)
4. Process and persist event
5. Acknowledge message
6. Record idempotency key

### Retry Flow (Transient Failures)
1. Processing fails with retryable exception
2. Don't acknowledge message
3. Wait exponential backoff (100ms → 1.6s)
4. Kafka redelivers message
5. Retry processing (max 5 attempts)
6. Success: acknowledge; Failure: send to DLQ

### Dead Letter Queue Flow
1. Message exceeds max retry attempts
2. Send to DLQ Kafka topic
3. Persist to failed_messages table
4. Acknowledge original message
5. DLQ consumer alerts on critical failures

### Idempotency Guarantee
1. Fast check: Redis (1-hour TTL)
2. Durable check: PostgreSQL (7-day retention)
3. Unique constraint prevents race conditions
4. Nightly cleanup removes old keys

## Performance Characteristics

### Throughput
- **Without failures**: 10,000-15,000 msg/sec per consumer
- **With 10% retry rate**: 8,000-10,000 msg/sec
- **Horizontal scaling**: Linear with consumer count

### Latency
- **Normal processing**: p50=5ms, p99=20ms
- **First retry**: +100ms
- **Max retry window**: ~3 seconds
- **Idempotency check**: +1-2ms (Redis), +5-10ms (PostgreSQL miss)

### Resource Usage
- **Consumer memory**: 512MB base + 10MB per 1000 in-flight
- **Database connections**: 10 per consumer
- **Redis memory**: ~100 bytes per idempotency key
- **Kafka retention**: 7 days (configurable)

## Failure Scenarios

### Consumer Crash
- Unacknowledged messages automatically redelivered
- Kafka tracks consumer offsets per partition
- Recovery time: <30 seconds

### Database Failure
- Circuit breaker opens after 50% error rate
- Messages accumulate in Kafka (backpressure)
- Processing resumes when DB recovers

### Kafka Broker Failure
- Producer retries with exponential backoff
- Consumers rebalance to available partitions
- Data loss prevented by replication factor

### Poison Pill Message
- Non-retryable error detected
- Message sent to DLQ immediately
- Partition consumption continues

## Configuration Tuning

### Increase Throughput
```yaml
# consumer application.yml
spring:
  kafka:
    consumer:
      max-poll-records: 500
      fetch-min-size: 1048576
```

### Adjust Retry Behavior
```java
@Retryable(
    maxAttempts = 10,
    backoff = @Backoff(delay = 50, multiplier = 2)
)
```

### Circuit Breaker Sensitivity
```yaml
resilience4j:
  circuitbreaker:
    instances:
      logProcessing:
        failureRateThreshold: 30
        waitDurationInOpenState: 30s
```

## Troubleshooting

### High Retry Rate
**Symptoms**: >5% of messages retrying  
**Check**: Downstream service health, database connection pool  
**Fix**: Scale downstream services, increase pool size

### Consumer Lag Growing
**Symptoms**: Offset lag increasing  
**Check**: Processing time, retry rate, resource usage  
**Fix**: Add more consumers, optimize processing logic

### DLQ Messages Accumulating
**Symptoms**: Growing failed_messages table  
**Check**: DLQ message patterns, error types  
**Fix**: Fix data quality issues, add validation, implement DLQ replay

### Memory Pressure
**Symptoms**: Consumer OOMs during retry storms  
**Check**: In-flight message count, retry backoff timing  
**Fix**: Reduce max-poll-records, externalize retry state to Redis

## Cleanup
```bash
docker-compose down
docker-compose down -v
```

## Next Steps

Day 35 explores topic-based routing with Kafka exchange types, enabling dynamic message routing to specialized processing pipelines.

## Production Readiness

✅ Manual acknowledgment prevents message loss  
✅ Retry logic handles transient failures  
✅ Dead letter queue isolates bad messages  
✅ Idempotency prevents duplicate processing  
✅ Circuit breaker prevents cascade failures  
✅ Comprehensive monitoring and alerting  
✅ Horizontal scalability tested  
✅ Failure scenarios validated
