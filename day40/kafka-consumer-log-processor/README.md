# Day 40: Kafka Consumer Log Processing System

## Overview
Production-ready Kafka consumer implementation demonstrating:
- Consumer groups with automatic partition assignment
- Manual offset management for at-least-once semantics
- Parallel processing with thread pools
- Retry mechanisms with exponential backoff
- Dead letter queue for poison pills
- Comprehensive monitoring and metrics

## Architecture

### Components
1. **API Gateway** (Port 8080): Entry point for log ingestion
2. **Log Producer** (Port 8081): Kafka producer service
3. **Log Consumer** (Port 8082): Multi-threaded consumer with processing pipeline
4. **Kafka**: Message broker with 12 partitions
5. **PostgreSQL**: Persistent log storage
6. **Redis**: Real-time aggregations
7. **Prometheus**: Metrics collection
8. **Grafana**: Visualization dashboards

### Data Flow
```
Client → API Gateway → Producer → Kafka → Consumer Group (3 instances)
                                            ↓
                                    Enrichment → PostgreSQL
                                            ↓
                                    Aggregation → Redis
                                            ↓
                                    (on failure) → DLQ
```

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Start Infrastructure
```bash
./setup.sh
```

### 2. Build Services
```bash
mvn clean package
```

### 3. Run Services
```bash
# Terminal 1: Producer
java -jar log-producer/target/log-producer-1.0.0.jar

# Terminal 2: Consumer
java -jar log-consumer/target/log-consumer-1.0.0.jar

# Terminal 3: Gateway
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

### 4. Run Tests
```bash
# Integration tests
./integration-tests/test-consumer-flow.sh

# Load test (1000 events)
./load-test.sh
```

## Consumer Configuration

### Key Settings
- **Consumer Group**: `log-processor-group`
- **Concurrency**: 3 consumer instances
- **Thread Pool**: 10 threads per instance
- **Batch Size**: 500 records
- **Max Poll Interval**: 5 minutes
- **Session Timeout**: 30 seconds

### Offset Management
Manual commit mode with batch-level acknowledgment:
```java
@KafkaListener(topics = "application-logs")
public void consume(ConsumerRecords<String, LogEvent> records, Acknowledgment ack) {
    // Process all records
    CompletableFuture.allOf(futures).join();
    
    // Commit offsets only after successful batch processing
    ack.acknowledge();
}
```

## Monitoring

### Grafana Dashboards
Access: http://localhost:3000 (admin/admin)

Key Metrics:
- Consumer processing rate
- Consumer lag by partition
- Processing latency (p50, p95, p99)
- DLQ message rate
- Rebalancing events

### Prometheus Queries
```
# Consumer throughput
rate(log_consumer_success_total[1m])

# Processing latency p99
histogram_quantile(0.99, rate(log_consumer_processing_time_bucket[5m]))

# Consumer lag
kafka_consumer_fetch_manager_records_lag_max
```

## Error Handling

### Retry Strategy
1. Attempt 1: Immediate processing
2. Attempt 2: 1 second delay
3. Attempt 3: 2 seconds delay
4. Attempt 4: 4 seconds delay
5. Failure: Send to DLQ

### Dead Letter Queue
Topic: `logs-dlq`
Retention: 7 days
Partitions: 3

## Performance Targets

### Throughput
- Single consumer: ~560 msg/sec
- 3 consumers (current): ~1,680 msg/sec
- 30 consumers (scaled): ~50,000 msg/sec

### Latency
- p50: <50ms
- p95: <150ms
- p99: <500ms

## Scaling Strategy

### Horizontal Scaling
```bash
# Scale to 6 consumers for 2x throughput
# Update docker-compose or K8s deployment
replicas: 6
```

### Partition Planning
Current: 12 partitions
Maximum parallelism: 12 consumers
Recommendation: Create topics with 2-3x expected max consumers

## Troubleshooting

### High Consumer Lag
```bash
# Check lag
docker exec -it $(docker ps -q -f name=kafka) kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group log-processor-group

# Solutions:
# 1. Scale out consumers
# 2. Increase processing threads
# 3. Optimize processing logic
```

### Rebalancing Issues
```bash
# Monitor rebalancing
grep "rebalance" log-consumer.log

# Tune timeouts in application.yml:
max.poll.interval.ms: 300000  # Increase if processing takes longer
session.timeout.ms: 30000     # Increase for unstable networks
```

## Real-World Connections

This implementation mirrors production patterns at:
- **Netflix**: 7 trillion messages/day through consumer groups
- **Uber**: Exactly-once semantics for fare calculations
- **LinkedIn**: 50,000 consumer instances across 300 clusters

## Next Steps - Day 41

Tomorrow we'll implement:
- Custom partition assignment strategies
- Consumer group rebalancing listeners
- Offset management across multiple topics
- Parallel consumer scaling patterns

## License
Educational use - System Design Course Day 40
