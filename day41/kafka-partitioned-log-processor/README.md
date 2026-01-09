# Day 41: Kafka Partitioning and Consumer Groups

## System Architecture

This system demonstrates production-grade Kafka partitioning and consumer group implementation for parallel log processing.

### Key Components

- **12-Partition Kafka Topic**: Enables horizontal scalability up to 12 consumer instances
- **Consumer Groups**: Automatic load balancing with sticky assignment strategy
- **Multi-Broker Cluster**: 3 Kafka brokers with replication factor 3
- **Partition Key Strategy**: Source-based partitioning for ordered processing per log source
- **Graceful Rebalancing**: Minimizes disruption during consumer scaling

## Architecture Highlights

### Partition Strategy
- **12 partitions** supporting 1-12 parallel consumers
- **Source-based keys** ensure all logs from same source go to same partition
- **Replication factor 3** with min in-sync replicas = 2
- **Sticky assignment** minimizes partition movement during rebalancing

### Consumer Configuration
- **3 consumer threads per instance** (vertical scaling)
- **Manual offset commit** for precise control
- **Session timeout 30s** with heartbeat 10s
- **Max poll interval 5 minutes** tolerates processing delays
- **Graceful shutdown** with 30-second drain period

### Monitoring
- **Per-partition lag tracking** every 30 seconds
- **Processing time metrics** for performance analysis
- **Partition distribution visibility** across consumer group
- **Auto-scaling triggers** based on lag thresholds

## Deployment

### 1. Start Infrastructure
```bash
./run-infrastructure.sh
```

Starts:
- 3 Kafka brokers (ports 9092-9094)
- PostgreSQL (port 5432)
- Redis (port 6379)
- Prometheus (port 9090)
- Grafana (port 3000)

### 2. Build Services
```bash
mvn clean package
```

### 3. Run Services

**Terminal 1 - Producer:**
```bash
cd log-producer
mvn spring-boot:run
```

**Terminal 2 - Consumer Instance 1:**
```bash
cd log-consumer
mvn spring-boot:run
```

**Terminal 3 - Consumer Instance 2:**
```bash
cd log-consumer
SERVER_PORT=8083 mvn spring-boot:run
```

**Terminal 4 - Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```

## Testing

### Integration Tests
```bash
cd integration-tests
./test-partitioning.sh
```

Tests:
1. Multi-source log distribution across partitions
2. Partition mapping verification
3. Consumer lag monitoring
4. Hot partition handling
5. Consumer health checks

### Load Testing
```bash
./load-test.sh
```

Generates 10,000 events from 8 different sources to test:
- Partition distribution
- Consumer group load balancing
- Throughput under load
- Lag accumulation patterns

## Monitoring

### Prometheus Metrics
Access: http://localhost:9090

Key queries:
```promql
# Events processed per partition
rate(log_events_processed_by_partition_total[1m])

# Consumer lag by partition
kafka_consumer_lag

# Average processing time
rate(log_processing_time_seconds_sum[1m]) / rate(log_processing_time_seconds_count[1m])
```

### Grafana Dashboard
Access: http://localhost:3000 (admin/admin)

Pre-configured dashboard shows:
- Events processed by partition
- Consumer lag trends
- Processing time distribution
- Consumer group health

## API Endpoints

### Producer
- `POST /api/logs` - Send log event
- `GET /api/logs/partition-mapping` - View partition assignments

### Consumer
- `GET /api/consumer/health` - Consumer health status
- `GET /api/consumer/lag` - Current lag per partition
- `GET /api/consumer/partition-distribution` - Event distribution
- `GET /api/consumer/assigned-partitions` - Consumer partition assignments

### Gateway
- `POST /api/logs` - Send log via gateway
- `GET /api/partition-mapping` - Partition mapping
- `GET /api/consumer/lag` - Consumer lag

## Scaling Strategies

### Horizontal Scaling
Add more consumer instances (up to 12):
```bash
cd log-consumer
SERVER_PORT=8084 mvn spring-boot:run
```

Kafka automatically rebalances partitions across all consumers.

### Vertical Scaling
Increase `concurrency` in `KafkaConsumerConfig`:
```java
factory.setConcurrency(5); // 5 threads per instance
```

### Partition Scaling
Increase partition count (requires topic recreation):
```java
.partitions(24) // Double partition count
```

## Key Patterns Demonstrated

1. **Semantic Partitioning**: Source-based keys maintain ordering where it matters
2. **Consumer Group Coordination**: Automatic partition assignment and rebalancing
3. **Sticky Assignment**: Minimizes state loss during rebalancing
4. **Lag Monitoring**: Per-partition lag tracking for scaling decisions
5. **Graceful Shutdown**: Proper handling of redeployments and failures

## Production Considerations

### Partition Count
- **Rule of thumb**: 2x expected max consumers
- **Our choice**: 12 partitions for 3-6 typical consumers
- **Trade-off**: More partitions = more parallelism but higher overhead

### Rebalancing Impact
- **Duration**: 3-5 seconds typical
- **Mitigation**: Sticky assignment, batched consumer updates
- **Monitoring**: Track rebalance frequency and duration

### Hot Partitions
- **Detection**: Monitor per-partition lag
- **Solution**: Add suffix to high-volume keys for distribution
- **Example**: `hot-source-1`, `hot-source-2`

### Consumer Failures
- **Detection time**: 30 seconds (session timeout)
- **Recovery time**: 3-5 seconds (rebalance)
- **Total**: 33-35 seconds to resume processing

## Cleanup
```bash
docker-compose down -v
```

## Next Steps: Day 42

Tomorrow we implement exactly-once processing semantics using:
- Transactional producers
- Idempotent consumers
- Offset management with transactions
- Dead letter queues for failures
