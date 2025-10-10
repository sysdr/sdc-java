# Day 11: Batching Log Shipper System

Production-grade distributed log processing system demonstrating intelligent batching patterns for network optimization.

## Architecture Overview

```
Log Producer → Log Shipper (Batching) → Kafka → Log Consumer → PostgreSQL
                                                                    ↓
                                                              API Gateway ← Redis
```

### Components

1. **Log Producer** (Port 8081): Generates synthetic logs at 10 logs/second
2. **Log Shipper** (Port 8082): Batches logs using dual-trigger strategy (1000 logs OR 5 seconds)
3. **Kafka**: Message broker for reliable log delivery
4. **Log Consumer** (Port 8083): Consumes batches and persists to PostgreSQL
5. **API Gateway** (Port 8080): Query API with Redis caching
6. **Prometheus** (Port 9090): Metrics collection
7. **Grafana** (Port 3000): Metrics visualization

## Quick Start

### Prerequisites
- Docker & Docker Compose
- 4GB RAM minimum
- Ports 8080-8083, 9090, 3000, 5432, 6379, 9092, 2181 available

### Deploy System

```bash
# Generate all files (if you haven't already)
chmod +x generate_system_files.sh
./generate_system_files.sh
cd log-batching-system

# Start system
./setup.sh

# Wait ~60 seconds for services to start
```

### Verify Deployment

```bash
# Run integration tests
./integration-tests/test-system.sh

# Check logs are flowing
curl http://localhost:8080/api/query/recent | jq '.[0:5]'
```

## Running Load Tests

```bash
# Generate 10,000 requests over 60 seconds
./load-test.sh

# Monitor in real-time
# Prometheus: http://localhost:9090
# Query: rate(logs_sent_total[1m])
```

## Key Metrics to Monitor

### Batching Efficiency
- **logs_sent_total**: Total logs successfully sent to Kafka
- **batches_sent_total**: Number of batches sent
- **Avg Batch Size**: `logs_sent_total / batches_sent_total`

### Performance
- **batch_flush_duration**: Time to flush batch to Kafka
- **buffer_size**: Current logs in buffer
- **buffer_remaining_capacity**: Space left in buffer

### Health
- **logs_dropped_total**: Logs dropped due to buffer full (should be 0)
- **kafka_send_failures**: Failed Kafka transmissions

## Prometheus Queries

```promql
# Throughput (logs/second)
rate(logs_sent_total[1m])

# Average batch size over 5 minutes
rate(logs_sent_total[5m]) / rate(batches_sent_total[5m])

# Buffer utilization percentage
(buffer_size / 10000) * 100

# Drop rate (should be 0)
rate(logs_dropped_total[1m])
```

## Testing Scenarios

### 1. Normal Load
```bash
# Default: 10 logs/second
# Expected: Batches flush every 5 seconds with ~50 logs/batch
curl http://localhost:8082/actuator/metrics/batches.sent
```

### 2. High Load
```bash
# Run load test
./load-test.sh

# Expected: Batches flush at 1000 logs (size trigger)
# Throughput: 166 logs/second → ~6 batches/second
```

### 3. Backpressure
```bash
# Stop consumer to create backpressure
docker-compose stop log-consumer

# Watch buffer fill up
watch -n 1 'curl -s http://localhost:8082/actuator/metrics/buffer.size'

# Expected: Buffer grows, then logs start dropping at capacity
```

### 4. Graceful Shutdown
```bash
# Stop shipper, check logs flush
docker-compose stop log-shipper

# Verify no data loss
curl http://localhost:8080/api/query/recent | jq 'length'
```

## Configuration

### Batching Parameters (log-shipper)
```yaml
batching:
  max-batch-size: 1000      # Size trigger
  buffer-capacity: 10000    # Max buffered logs
```

### Time Trigger
```java
@Scheduled(fixedDelay = 5000)  // 5-second flush interval
```

### Kafka Producer
```yaml
spring:
  kafka:
    producer:
      acks: all          # Wait for all replicas
      retries: 3         # Retry failed sends
```

## Performance Benchmarks

### Without Batching
- Network calls: 10 requests/second
- Bandwidth: ~5 KB/second
- Kafka load: 10 messages/second

### With Batching (1000 logs, 5 seconds)
- Network calls: ~2 requests/second (5x reduction)
- Bandwidth: ~2 KB/second (2.5x reduction)
- Kafka load: 2 messages/second (5x reduction)

### At 1000 logs/second
- Without batching: 1000 Kafka requests/second
- With batching: 5 Kafka requests/second (200x reduction)

## Troubleshooting

### Logs not flowing
```bash
# Check producer is sending
curl http://localhost:8081/actuator/metrics/logs.generated

# Check shipper is receiving
curl http://localhost:8082/actuator/metrics/logs.received

# Check Kafka connectivity
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### High memory usage
```bash
# Check buffer size
curl http://localhost:8082/actuator/metrics/buffer.size

# If high: consumer is slow or Kafka is down
docker-compose logs log-consumer
```

### Data loss
```bash
# Check drop rate (should be 0)
curl http://localhost:8082/actuator/metrics/logs.dropped

# If >0: increase buffer capacity or consumer throughput
```

## Clean Up

```bash
# Stop all services
docker-compose down

# Remove volumes (deletes data)
docker-compose down -v

# Remove images
docker-compose down --rmi all
```

## System Design Insights

### 1. Dual-Trigger Batching
Combines size-based (throughput) and time-based (latency) triggers for optimal performance.

### 2. Bounded Buffers
Prevents memory leaks during backpressure by dropping logs when buffer fills.

### 3. Graceful Degradation
System remains operational under load, preferring data loss over cascade failures.

### 4. At-Least-Once Semantics
Kafka acks=all ensures batches are durable, with 5-10s potential loss window during crashes.

### 5. Monitoring First
Comprehensive metrics enable observability and capacity planning.

## Next Steps

- **Day 12**: Add compression (gzip/snappy) to reduce bandwidth by 5-10x
- **Day 13**: Implement circuit breakers for Kafka failures
- **Day 14**: Add distributed tracing with OpenTelemetry
- **Day 15**: Horizontal scaling with multiple shipper instances

## Architecture Decisions

### Why Kafka?
- Durable message storage
- Horizontal scalability
- Consumer lag visibility
- Replay capability

### Why Bounded Queues?
- Predictable memory usage
- Fail-fast under backpressure
- Clear capacity planning

### Why Dual Triggers?
- Latency guarantee during quiet periods
- Throughput optimization during peaks
- Predictable worst-case behavior

## Additional Resources

- [Kafka Performance Best Practices](https://kafka.apache.org/documentation/#producerconfigs)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Batching in Distributed Systems (Paper)](https://www.usenix.org/conference/nsdi14/technical-sessions/presentation/li_cheng)
