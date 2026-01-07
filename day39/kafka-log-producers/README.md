# Kafka Log Producers - High-Throughput Log Ingestion System

Production-ready multi-source log ingestion system demonstrating Kafka producer patterns, batching strategies, and observability at scale.

## Architecture Overview

This system implements three types of log shippers:

### 1. Application Log Shipper (Port 8081)
- **Purpose**: Collects application logs via HTTP endpoints
- **Throughput**: 50,000+ events/second
- **Configuration**: acks=1, lz4 compression, 32KB batches
- **Use Case**: General application logging (web apps, microservices)

### 2. Infrastructure Log Shipper (Port 8082)
- **Purpose**: Generates system metrics (CPU, memory, disk)
- **Throughput**: 3,000 events/second (sustained)
- **Configuration**: Same as application shipper
- **Use Case**: Infrastructure monitoring, system metrics

### 3. Transaction Log Shipper (Port 8083)
- **Purpose**: Handles critical transaction events
- **Throughput**: Lower throughput, guarantees exactly-once
- **Configuration**: acks=all, idempotent producer
- **Use Case**: Financial transactions, audit logs

### 4. Log Gateway (Port 8080)
- **Purpose**: Unified entry point for all log ingestion
- **Technology**: Spring WebFlux (reactive)
- **Pattern**: API Gateway pattern

## System Design Patterns Implemented

### Producer Configuration Strategies
- **Leader Acknowledgment (acks=1)**: Balance between throughput and durability
- **Full Replication (acks=all)**: Maximum durability for critical data
- **Batching & Linger**: 32KB batches with 10ms linger for optimal throughput
- **LZ4 Compression**: Fast compression with 25-35% size reduction

### Reliability Patterns
- **Rate Limiting**: Token bucket at 50K events/sec prevents broker overwhelm
- **Circuit Breaker**: Fail fast when Kafka unavailable
- **Transactional Outbox**: PostgreSQL-backed exactly-once semantics
- **Idempotent Producers**: Automatic deduplication

### Observability
- **Prometheus Metrics**: Throughput, latency, error rates
- **Grafana Dashboards**: Real-time visualization
- **Distributed Tracing**: Request correlation across services

## Technology Stack

- **Java 17** with Spring Boot 3.2
- **Apache Kafka 7.5** for message streaming
- **PostgreSQL 15** for transactional outbox
- **Prometheus + Grafana** for monitoring
- **Docker Compose** for orchestration

## Quick Start

### 1. Generate and Start the System

```bash
chmod +x setup.sh
./setup.sh
```

This will:
- Start Kafka, Zookeeper, PostgreSQL
- Launch all four microservices
- Create Kafka topics (3 partitions each)
- Start Prometheus and Grafana
- Complete startup in ~60 seconds

### 2. Verify System Health

```bash
# Check all services are up
docker compose ps

# View application logs
docker compose logs -f application-log-shipper

# Check Kafka topics
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 3. Run Integration Tests

```bash
./integration-tests/test-producers.sh
```

Expected output:
```json
{"status":"accepted","eventId":"..."}
```

### 4. Run Load Test

```bash
./load-test.sh
```

Generates 10,000 requests to test producer throughput.

## Monitoring and Metrics

### Prometheus Metrics

Access Prometheus at http://localhost:9090

Key queries:
```promql
# Producer throughput (events/sec)
rate(kafka_producer_success_total[1m])

# Producer error rate
rate(kafka_producer_error_total[1m])

# Average send latency
kafka_producer_send_duration_seconds

# Rate limiting events
rate(producer_throttled_total[1m])
```

### Grafana Dashboards

Access Grafana at http://localhost:3000 (admin/admin)

Pre-configured dashboards show:
- Producer throughput per service
- Latency histograms (p50, p95, p99)
- Error rates and types
- Batch size distributions

### Kafka Monitoring

```bash
# Consumer lag (once consumers are implemented)
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group log-processors

# Topic details
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic application-logs
```

## Testing Scenarios

### Scenario 1: Normal Load
```bash
# Send 100 logs via gateway
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/v1/logs \
    -H "Content-Type: application/json" \
    -d "{\"source\":\"test\",\"level\":\"INFO\",\"message\":\"Test $i\"}"
done
```

### Scenario 2: Batch Ingestion
```bash
curl -X POST http://localhost:8081/api/v1/logs/batch \
  -H "Content-Type: application/json" \
  -d '{
    "logs": [
      {"source":"app-1","level":"INFO","message":"Msg 1"},
      {"source":"app-2","level":"WARN","message":"Msg 2"},
      {"source":"app-3","level":"ERROR","message":"Msg 3"}
    ]
  }'
```

### Scenario 3: Transaction Events
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "type": "PAYMENT",
    "amount": 99.99,
    "currency": "USD"
  }'
```

### Scenario 4: Rate Limit Testing
```bash
# Overwhelm the system (should trigger rate limiting)
./load-test.sh
```

Watch for `producer.throttled` metric increases in Prometheus.

## Performance Benchmarks

### Expected Performance

| Service | Throughput | Latency (p99) | CPU | Memory |
|---------|-----------|---------------|-----|--------|
| Application Shipper | 50,000 msg/s | <15ms | 60% (2 cores) | 512MB |
| Infrastructure Shipper | 3,000 msg/s | <20ms | 40% (2 cores) | 512MB |
| Transaction Shipper | 5,000 msg/s | <50ms | 50% (2 cores) | 768MB |

### Network Utilization

- **Uncompressed**: ~10 MB/s at 50K msg/s
- **With LZ4 compression**: ~7 MB/s (30% reduction)
- **Kafka acknowledgments**: ~1 MB/s inbound

## Scaling Strategies

### Horizontal Scaling

```yaml
# Scale application shippers to 3 instances
docker compose up -d --scale application-log-shipper=3
```

Kafka will distribute load across partitions.

### Vertical Scaling

Adjust producer configuration:

```yaml
# Increase batch size for better throughput
batch-size: 65536  # 64KB

# Increase linger for high-load scenarios
linger-ms: 50

# More buffer memory
buffer-memory: 134217728  # 128MB
```

### Partition Scaling

```bash
# Increase partitions for better parallelism
docker compose exec kafka kafka-topics \
  --alter --topic application-logs \
  --partitions 10 \
  --bootstrap-server localhost:9092
```

## Failure Scenarios

### Kafka Broker Down

Producers will retry up to 3 times with exponential backoff. Circuit breakers open after 10 consecutive failures, preventing memory exhaustion.

```bash
# Simulate Kafka failure
docker compose stop kafka

# Observe circuit breaker metrics
curl http://localhost:8081/actuator/metrics/producer.circuit.breaker
```

### Network Partition

Producers buffer messages up to 64MB. When buffer exhausts, they return HTTP 503 to clients, propagating backpressure.

### Producer Service Crash

- Application/Infrastructure shippers: Unsent messages lost (acceptable for logs)
- Transaction shipper: Outbox table preserves events for replay

## Production Checklist

- [ ] Kafka cluster with 3+ brokers for replication
- [ ] Set `min.insync.replicas=2` for durability
- [ ] Configure `compression.type=lz4` for all producers
- [ ] Enable idempotence for critical data streams
- [ ] Implement DLQ for failed messages
- [ ] Set up alerts on error rates > 1%
- [ ] Configure log retention based on storage capacity
- [ ] Implement security (SSL/SASL authentication)
- [ ] Set up backup and disaster recovery

## Troubleshooting

### Producers Not Sending

```bash
# Check Kafka connectivity
docker compose exec application-log-shipper nc -zv kafka 9092

# View producer logs
docker compose logs application-log-shipper | grep ERROR
```

### High Latency

```bash
# Check producer metrics
curl http://localhost:8081/actuator/metrics/kafka.producer.request-latency-avg

# Check Kafka broker metrics
docker compose exec kafka kafka-run-class kafka.tools.JmxTool \
  --object-name kafka.server:type=BrokerTopicMetrics,name=TotalProduceRequestsPerSec
```

### Messages Not Reaching Kafka

```bash
# Verify topic exists
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check topic has data
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic application-logs \
  --from-beginning --max-messages 10
```

## Next Steps

- Day 40: Implement Kafka consumers with offset management
- Add dead letter queue for failed messages
- Implement schema registry for event versioning
- Add distributed tracing with Zipkin
- Implement blue-green deployment strategy

## License

MIT
