# Dead Letter Queue Log Processing System

A production-grade distributed log processing system demonstrating dead letter queue patterns for handling failed message processing.

## Architecture

### Components
- **Log Producer** (Port 8081): REST API for publishing log events
- **Log Consumer** (Port 8082): Kafka consumer with retry and DLQ logic
- **API Gateway** (Port 8080): DLQ management and reprocessing API
- **Kafka**: Message broker with topics: log-events, log-events-retry, log-events-dlq
- **PostgreSQL**: Persistent storage for processed logs
- **Prometheus + Grafana**: Monitoring and visualization

### DLQ Flow
1. Consumer attempts to process message
2. On failure, classifies error type (VALIDATION, TIMEOUT, PROCESSING)
3. Transient errors → retry topic with exponential backoff (2s, 4s, 8s)
4. Permanent errors → immediate DLQ routing
5. After 3 retry attempts → DLQ with full diagnostic context
6. DLQ messages can be inspected and reprocessed via API

## Quick Start

### 1. Start Infrastructure
```bash
./setup.sh
```

Wait for "Infrastructure ready!" message.

### 2. Build Services
```bash
# Terminal 1 - Producer
cd log-producer
mvn clean install
mvn spring-boot:run

# Terminal 2 - Consumer
cd log-consumer
mvn clean install
mvn spring-boot:run

# Terminal 3 - Gateway
cd api-gateway
mvn clean install
mvn spring-boot:run
```

### 3. Run Tests
```bash
./integration-tests/test-dlq.sh
```

### 4. Load Test
```bash
./load-test.sh
```

## API Endpoints

### Producer
- `POST /api/logs/event` - Publish single log event
- `POST /api/logs/batch` - Publish batch with configurable failure rate

### DLQ Management
- `GET /api/dlq/messages` - List DLQ messages (filter by error type)
- `GET /api/dlq/messages/{id}` - Get specific message details
- `POST /api/dlq/reprocess/{id}` - Reprocess single message
- `POST /api/dlq/reprocess/batch` - Bulk reprocess with rate limiting
- `GET /api/dlq/stats` - DLQ statistics and metrics

## Testing Failure Scenarios

### Validation Error (Immediate DLQ)
```bash
curl -X POST http://localhost:8081/api/logs/event \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR",
    "service": "test",
    "message": "validation error",
    "shouldFail": true
  }'
```

### Timeout Error (Retry then DLQ)
```bash
curl -X POST http://localhost:8081/api/logs/event \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR",
    "service": "test",
    "message": "timeout error",
    "shouldFail": true
  }'
```

### Batch with Failures
```bash
curl -X POST http://localhost:8081/api/logs/batch \
  -H "Content-Type: application/json" \
  -d '{
    "count": 1000,
    "failureRate": 5
  }'
```

## Monitoring

### Prometheus Metrics
- `dlq_messages_total{type="VALIDATION"}` - DLQ messages by error type
- `consumer_messages_processed` - Successfully processed messages
- `consumer_messages_failed` - Failed message count

### Grafana Dashboard
Access at http://localhost:3000 (admin/admin)
- Real-time DLQ ingestion rates
- Failure distribution by error type
- Message processing latency
- Retry attempt patterns

## Production Patterns

### Error Classification
- **VALIDATION**: Schema errors, malformed data → immediate DLQ
- **TIMEOUT**: Network, database timeouts → retry with backoff
- **PROCESSING**: Business logic errors → limited retries
- **UNKNOWN**: Unclassified errors → default retry policy

### Retry Strategy
- Attempt 1: 2 second delay
- Attempt 2: 4 second delay
- Attempt 3: 8 second delay
- After 3 attempts: Route to DLQ

### Capacity Planning
- DLQ sized for 10x normal failure rate
- Monitor DLQ rate > 5% as alert threshold
- Retention: 7 days for investigation
- Reprocessing rate-limited to avoid thundering herd

## Performance Benchmarks

Expected throughput (single consumer):
- Normal processing: 10,000 msg/sec
- With 1% failure rate: 9,900 msg/sec (100 to DLQ)
- DLQ overhead: <5ms per failed message
- Retry latency: P99 < 50ms (excluding backoff time)

## Troubleshooting

### Messages not reaching DLQ
- Check consumer logs for exception handling
- Verify retry-count header incrementing
- Confirm DLQ topic exists: `docker exec dlq-log-system-kafka-1 kafka-topics --list --bootstrap-server localhost:9092`

### High DLQ rate
- Query DLQ stats: `curl http://localhost:8080/api/dlq/stats`
- Check error type distribution
- Review recent code deployments
- Investigate database/network health

### Reprocessing failures
- Ensure original issue resolved before reprocessing
- Check consumer logs for new errors
- Verify message payload still valid
- Consider manual transformation if schema changed

## Cleanup
```bash
docker-compose down -v
```

## Next Steps
- Implement priority queues (Day 37)
- Add schema validation with Avro
- Integrate distributed tracing
- Implement circuit breakers for external dependencies
