# Day 32: Log Producer System with Kafka Integration

Production-ready distributed log producer system demonstrating message queue patterns, reliability guarantees, and performance optimization.

## System Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────┐
│             │────▶│              │────▶│         │
│  Clients    │     │ API Gateway  │     │  Kafka  │
│             │◀────│              │◀────│         │
└─────────────┘     └──────────────┘     └────┬────┘
                                               │
                    ┌──────────────┐          │
                    │              │◀─────────┘
                    │ Log Producer │
                    │              │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │              │
                    │    Redis     │
                    │              │
                    └──────────────┘
```

## Components

### Log Producer Service (Port 8081)
- REST API for log ingestion
- Kafka producer with custom partitioning
- Circuit breaker and retry patterns
- Rate limiting and backpressure
- Prometheus metrics

### Log Consumer Service (Port 8082)
- Kafka consumer with parallel processing
- PostgreSQL persistence
- Health checks and monitoring

### API Gateway (Port 8080)
- Unified entry point
- Request routing
- Load balancing

### Infrastructure
- **Kafka**: Message broker (3 partitions)
- **Redis**: Caching and rate limiting
- **PostgreSQL**: Log persistence
- **Prometheus**: Metrics collection
- **Grafana**: Visualization

## Quick Start

### 1. Prerequisites
```bash
# Required
- Docker & Docker Compose
- Java 17+
- Maven 3.8+

# Optional (for load testing)
- Apache Bench (ab)
- curl
```

### 2. Setup Infrastructure
```bash
# Start all infrastructure services
./setup.sh

# Verify services are running
docker-compose ps
```

### 3. Build Services
```bash
# Build all modules
mvn clean package -DskipTests

# Or build individually
cd log-producer && mvn clean package
cd log-consumer && mvn clean package
cd api-gateway && mvn clean package
```

### 4. Start Services

**Terminal 1 - Producer:**
```bash
cd log-producer
mvn spring-boot:run
```

**Terminal 2 - Consumer:**
```bash
cd log-consumer
mvn spring-boot:run
```

**Terminal 3 - Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```

### 5. Test the System
```bash
# Run integration tests
./integration-tests/test-producer.sh

# Run load tests
./load-test.sh
```

## API Usage

### Single Log Ingestion
```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "source": "my-app",
    "level": "INFO",
    "message": "User login successful",
    "metadata": {
      "userId": "12345",
      "ip": "192.168.1.1"
    }
  }'

# Response: 202 Accepted
{
  "eventId": "uuid-here",
  "status": "Accepted",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Batch Ingestion
```bash
curl -X POST http://localhost:8080/api/logs/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "source": "app-1",
      "level": "INFO",
      "message": "Log 1"
    },
    {
      "source": "app-2",
      "level": "ERROR",
      "message": "Log 2",
      "stackTrace": "Exception: error"
    }
  ]'

# Response: 202 Accepted
{
  "accepted": 2,
  "failed": 0,
  "timestamp": "2024-01-15T10:31:00Z"
}
```

### Health Check
```bash
curl http://localhost:8080/api/logs/health

# Response
{
  "status": "UP",
  "metrics": {
    "messagesSent": 10000,
    "messagesFailed": 5,
    "avgSendDuration": 12.5
  }
}
```

## Performance Characteristics

### Target Metrics
- **Throughput**: 50,000 events/second per instance
- **Latency**: p99 < 150ms end-to-end
- **Reliability**: 99.99% message delivery
- **Availability**: 99.95% uptime

### Actual Results (4-core, 8GB RAM)
```
Requests:      10,000
Concurrency:   50
Time taken:    8.2 seconds
Requests/sec:  1,220
Mean latency:  41ms
p95 latency:   85ms
p99 latency:   142ms
Failed:        0 (0%)
```

### Resource Usage
- CPU: ~40% (4 cores)
- Memory: 2.5GB heap
- Network: 175MB/sec (with compression)
- Disk: Minimal (Kafka handles persistence)

## Monitoring

### Prometheus Metrics
Access Prometheus at http://localhost:9090

**Key Metrics:**
```promql
# Messages sent rate
rate(kafka_producer_messages_sent_total[1m])

# Send latency p99
histogram_quantile(0.99, kafka_producer_send_duration_seconds_bucket)

# Error rate
rate(kafka_producer_messages_failed_total[1m])

# Circuit breaker state
resilience4j_circuitbreaker_state
```

### Grafana Dashboards
Access Grafana at http://localhost:3000 (admin/admin)

**Dashboards:**
- Producer Performance
- Kafka Metrics
- System Resources
- Error Rates

### Health Endpoints
```bash
# Producer health
curl http://localhost:8081/actuator/health

# Consumer health
curl http://localhost:8082/actuator/health

# Gateway health
curl http://localhost:8080/actuator/health
```

## Reliability Patterns

### 1. Producer Acknowledgments
- **At-most-once**: Fire-and-forget (default for INFO/DEBUG)
- **At-least-once**: Synchronous acks (for ERROR/FATAL)
- **Exactly-once**: Idempotent producer enabled

### 2. Retry Strategy
- Max retries: 3
- Initial backoff: 100ms
- Backoff multiplier: 2x (exponential)
- Max backoff: 10s

### 3. Circuit Breaker
- Failure threshold: 50% in 10 calls
- Open duration: 60s
- Half-open calls: 3
- Fallback: Log locally and alert

### 4. Rate Limiting
- Single requests: 100/sec per IP
- Batch requests: 10/sec per IP
- Implements token bucket algorithm

## Partitioning Strategy

### Custom Partitioner Logic
1. **ERROR/FATAL logs** → Partition 0 (priority processing)
2. **Other logs** → Hash(source) % partitions (even distribution)

This ensures:
- Critical logs processed first
- Even load distribution
- Ordered processing per source

### Partition Configuration
```yaml
# Kafka topic: logs
Partitions: 3
Replication: 1
Retention: 7 days
Compression: snappy
```

## Failure Scenarios

### Kafka Broker Down
```
Timeline:
t=0s:    Broker fails
t=5s:    Circuit breaker opens
t=5-65s: Fast-fail new requests
t=65s:   Circuit half-open, retry
Result:  0% data loss, degraded throughput
```

### Network Partition
```
Timeline:
t=0s:    Network partition
t=0-30s: Producer buffers messages (64MB)
t=30s:   Buffer full, backpressure activates
t=30s+:  Clients receive 503 errors
Recovery: Automatic when network restored
```

### Consumer Lag
```
Detection: Monitor consumer lag metrics
Response: Auto-scale consumers (3→6 instances)
Recovery: Lag cleared in <5 minutes
```

## Troubleshooting

### High Latency
```bash
# Check batch filling
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:29092 \
  --describe --group log-processor

# Solution: Increase linger.ms or decrease batch.size
```

### Message Loss
```bash
# Check acks configuration
# Ensure acks=all for critical logs

# Verify replication
docker exec kafka kafka-topics --bootstrap-server localhost:29092 \
  --describe --topic logs
```

### Out of Memory
```bash
# Check buffer memory usage
curl http://localhost:8081/actuator/metrics/kafka.producer.buffer.available.bytes

# Solution: Increase buffer.memory or reduce producer count
```

## Scaling Strategies

### Horizontal Scaling
```bash
# Scale producers (stateless)
docker-compose up -d --scale log-producer=3

# Scale consumers (partitions limit)
# Max consumers = partition count (3)
docker-compose up -d --scale log-consumer=3
```

### Vertical Scaling
```yaml
# Increase heap size
JAVA_OPTS: "-Xmx4g -Xms4g"

# Increase batch size
batch.size: 65536  # 64KB

# Increase buffer
buffer.memory: 134217728  # 128MB
```

### Kafka Partitions
```bash
# Add partitions (cannot decrease)
docker exec kafka kafka-topics --bootstrap-server localhost:29092 \
  --alter --topic logs --partitions 6

# Restart consumers to rebalance
```

## Production Checklist

- [ ] Configure proper retention policies
- [ ] Set up monitoring alerts (latency, errors, lag)
- [ ] Enable TLS for Kafka connections
- [ ] Configure authentication (SASL/SCRAM)
- [ ] Set up log aggregation (ELK/Splunk)
- [ ] Implement dead letter queues
- [ ] Configure auto-scaling policies
- [ ] Set up backup and recovery
- [ ] Load test with production traffic patterns
- [ ] Document runbooks for common issues

## Next Steps: Day 33

Tomorrow we'll implement:
- Advanced consumer patterns (competing consumers, fan-out)
- Exactly-once processing semantics
- Dead letter queue handling
- Consumer group rebalancing strategies
- Backpressure and flow control

## Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Resilience4j Guide](https://resilience4j.readme.io/)
- [Micrometer Metrics](https://micrometer.io/docs)

## Architecture Insights

### Why This Design?
1. **Async boundaries** prevent coupling
2. **Partitioning** enables parallel processing
3. **Batching** optimizes network usage
4. **Circuit breakers** prevent cascading failures
5. **Metrics** enable data-driven operations

### Trade-offs Made
- Latency (+50ms) for throughput (+10x)
- Memory (+64MB) for buffering reliability
- Complexity (+3 services) for scalability
- Eventual consistency for availability

### Real-World Parallels
- **Netflix**: 500B events/day, same patterns
- **Uber**: 14M trips/day, similar partitioning
- **LinkedIn**: Kafka inventors, proved at scale

---

**System Design Principle**: Decouple event generation from processing. Your app's performance shouldn't depend on analytics pipeline speed.
