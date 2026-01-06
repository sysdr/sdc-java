# Day 38: Kafka Cluster for Log Streaming

Production-grade distributed log streaming platform with multi-broker Kafka cluster, comprehensive monitoring, and automated health checks.

## Architecture

This system implements a 3-broker Kafka cluster with:
- **ZooKeeper Ensemble**: 3-node quorum for cluster coordination
- **Kafka Brokers**: 3 brokers with replication factor of 3
- **Topic Configuration**: Partitioned topics for parallel processing
- **Health Monitoring**: Automated cluster health checks every 30 seconds
- **Load Testing**: Configurable throughput testing framework
- **Observability**: Prometheus metrics + Grafana dashboards

## System Components

### Infrastructure
- **ZooKeeper** (3 nodes): Cluster metadata and coordination
- **Kafka** (3 brokers): Distributed log storage and streaming
- **Kafka UI**: Web-based cluster management
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization

### Application Services
- **kafka-admin-service** (port 8081): Topic management and administration
- **kafka-health-service** (port 8082): Cluster health monitoring
- **load-test-service** (port 8083): Performance testing and benchmarking

## Quick Start

### 1. Start the Cluster

```bash
chmod +x setup.sh
./setup.sh
```

Wait 2-3 minutes for all services to start and become healthy.

### 2. Verify Cluster Health

```bash
# Check cluster status
curl http://localhost:8082/api/health

# List topics
curl http://localhost:8081/api/topics

# View Kafka UI
open http://localhost:8080
```

### 3. Run Load Tests

```bash
chmod +x load-test.sh
./load-test.sh
```

Expected results:
- 1,000 events: ~5,000-10,000 events/sec
- 10,000 events: ~20,000-30,000 events/sec
- 50,000 events: ~40,000-50,000 events/sec

### 4. View Metrics

```bash
# Prometheus
open http://localhost:9090

# Grafana (admin/admin)
open http://localhost:3000
```

## Topic Configuration

### log-events (General Application Logs)
- **Partitions**: 12
- **Replication Factor**: 3
- **Retention**: 7 days
- **Compression**: Snappy
- **Use Case**: High-volume application logs

### critical-logs (High-Priority Alerts)
- **Partitions**: 4
- **Replication Factor**: 3
- **Retention**: 14 days
- **Use Case**: Critical system alerts requiring immediate attention

### audit-logs (Compliance Logs)
- **Partitions**: 6
- **Replication Factor**: 3
- **Retention**: 30 days (with compaction)
- **Use Case**: Regulatory compliance and audit trails

## Performance Benchmarks

### Achieved Throughput
- **Single Producer**: 40,000-50,000 events/sec
- **Batch Size**: 16KB (configurable)
- **Compression**: Snappy (~60% reduction)
- **Latency**: p99 < 20ms

### Resource Usage (per broker)
- **CPU**: 2-4 cores
- **Memory**: 4GB heap + 2GB page cache
- **Disk**: 100GB (7 days retention at 50K events/sec)
- **Network**: 100-200 Mbps sustained

## Monitoring

### Key Metrics

**Cluster Health**
```promql
kafka_cluster_brokers_healthy
kafka_cluster_topics_healthy
```

**Producer Performance**
```promql
rate(kafka_producer_success_total[1m])
kafka_producer_send_time_seconds
```

**Consumer Lag**
```promql
kafka_consumer_lag{group="log-processor"}
```

### Alert Thresholds
- **Under-replicated partitions**: Alert if > 0
- **Consumer lag**: Alert if > 10,000 messages
- **Broker health**: Alert if < 3 healthy brokers
- **Request queue**: Alert if > 100 pending requests

## Failure Testing

### Broker Failure
```bash
# Stop broker 1
docker-compose stop kafka-1

# Verify cluster still operates
curl http://localhost:8082/api/health

# Restart broker
docker-compose start kafka-1
```

Expected behavior: Leadership redistributes, no data loss, ~10-30 second recovery.

### Network Partition
```bash
# Simulate network partition
docker network disconnect kafka-log-streaming-cluster_kafka-network kafka-1

# Check cluster state
curl http://localhost:8082/api/health

# Restore network
docker network connect kafka-log-streaming-cluster_kafka-network kafka-1
```

Expected behavior: Partitioned broker becomes isolated, majority continues operating.

## API Endpoints

### Admin Service (8081)
```bash
# List all topics
GET /api/topics

# Get topic details
GET /api/topics/{topicName}

# Create topic
POST /api/topics?topicName=new-topic&partitions=12&replicationFactor=3

# Delete topic
DELETE /api/topics/{topicName}
```

### Health Service (8082)
```bash
# Full cluster health
GET /api/health

# Status only
GET /api/health/status
```

### Load Test Service (8083)
```bash
# Run load test
POST /api/loadtest/run?eventCount=10000&topic=log-events

# Get statistics
GET /api/loadtest/stats

# Reset counters
POST /api/loadtest/reset
```

## Scaling Strategies

### Horizontal Scaling

**Add more partitions:**
```bash
# Increase parallelism for existing topics
curl -X POST "http://localhost:8081/api/topics?topicName=log-events-v2&partitions=24&replicationFactor=3"
```

**Add more brokers:**
```yaml
# docker-compose.yml
kafka-4:
  # ... same config as kafka-1-3 but KAFKA_BROKER_ID: 4
```

**Add more consumers:**
```bash
# Scale consumer groups (max = partition count)
# With 12 partitions, can run up to 12 consumers
```

### Vertical Scaling

**Increase broker resources:**
```yaml
kafka-1:
  environment:
    KAFKA_HEAP_OPTS: "-Xms8g -Xmx8g"  # Increase heap
    KAFKA_NUM_NETWORK_THREADS: 16     # More network threads
    KAFKA_NUM_IO_THREADS: 32          # More I/O threads
```

## Production Checklist

- [ ] ZooKeeper ensemble has odd number of nodes (3, 5, or 7)
- [ ] Kafka brokers spread across availability zones
- [ ] Replication factor ≥ 3 for critical topics
- [ ] min.insync.replicas = 2 for durability
- [ ] Monitoring alerts configured
- [ ] Backup and disaster recovery plan
- [ ] Security configured (SASL/TLS)
- [ ] Resource limits set (CPU, memory, disk)
- [ ] Log retention policies defined
- [ ] Consumer lag monitoring active

## Troubleshooting

### Cluster won't start
```bash
# Check ZooKeeper quorum
docker-compose logs zookeeper-1 zookeeper-2 zookeeper-3

# Verify network connectivity
docker network inspect kafka-log-streaming-cluster_kafka-network

# Reset and restart
docker-compose down -v
./setup.sh
```

### Under-replicated partitions
```bash
# Check broker logs
docker-compose logs kafka-1 | grep "Under replicated"

# Verify all brokers are healthy
curl http://localhost:8082/api/health

# Restart lagging broker
docker-compose restart kafka-2
```

### High consumer lag
```bash
# Check consumer group status via Kafka UI
open http://localhost:8080

# Add more consumers (up to partition count)
# Optimize consumer processing logic
# Consider increasing partition count
```

## Next Steps

Tomorrow (Day 39), we'll build Kafka producers that integrate with application logging frameworks, implementing:
- Log4j2 Kafka appenders
- Structured logging with MDC
- Batching and backpressure handling
- Guaranteed delivery semantics

## References

- **Kafka Documentation**: https://kafka.apache.org/documentation/
- **Confluent Best Practices**: https://docs.confluent.io/platform/current/kafka/deployment.html
- **LinkedIn Kafka Blog**: https://engineering.linkedin.com/kafka
- **Netflix Keystone**: https://netflixtechblog.com/keystone-real-time-stream-processing-platform-a3ee651812a

## License

MIT License - Use freely for learning and production deployments.
