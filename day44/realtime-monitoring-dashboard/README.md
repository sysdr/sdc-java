# Day 44: Real-Time Monitoring Dashboard with Kafka Streams

## Overview

A production-grade real-time monitoring system demonstrating Kafka Streams for stateful stream processing. The system processes 40,000+ events per second, performs windowed aggregations, and serves live metrics through interactive queries.

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│ Log         │────▶│   Kafka     │────▶│  Stream          │
│ Producer    │     │  (12 parts) │     │  Processor       │
│ (40k/sec)   │     │             │     │  (Aggregations)  │
└─────────────┘     └─────────────┘     └──────────────────┘
                                                  │
                                                  │ Interactive Queries
                                                  ▼
                                         ┌──────────────────┐
                                         │  Dashboard API   │
                                         │  (WebSockets)    │
                                         └──────────────────┘
                                                  │
                                                  ▼
                                         ┌──────────────────┐
                                         │   Web UI         │
                                         │   (Real-time)    │
                                         └──────────────────┘
```

## System Components

### 1. Log Producer (`log-producer`)
- Generates 40,000 events/second across 8 endpoints
- Realistic HTTP request simulation with weighted status codes
- Response times follow normal distribution

### 2. Stream Processor (`stream-processor`)
- Kafka Streams topology with multiple aggregations
- **Request counts**: 1-minute tumbling windows per endpoint
- **Detailed metrics**: Percentiles (P50, P95, P99), error rates
- **Status codes**: 5-minute hopping windows
- **Regional metrics**: 1-minute tumbling windows
- RocksDB-backed state stores with changelog topics

### 3. Dashboard API (`dashboard-api`)
- REST endpoints for on-demand queries
- WebSocket server for real-time metric streaming
- Polls stream processor every 2 seconds
- Redis for session state

### 4. Dashboard UI
- Real-time visualization with Chart.js
- WebSocket client for live updates
- Request trends, error tracking, regional distribution

## Key Patterns Demonstrated

### Stateful Stream Processing
- Local state stores with RocksDB
- Changelog topics for fault tolerance
- State restoration after crashes
- Interactive queries for real-time serving

### Windowed Aggregations
- Tumbling windows: Non-overlapping 1-minute buckets
- Hopping windows: Overlapping 5-minute windows
- Time-based state eviction
- Grace periods for late arrivals

### Exactly-Once Semantics
- Transactional processing
- Atomic read-process-write operations
- No duplicate counts after restarts

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Maven 3.9+
- Java 17+
- 8GB RAM minimum

### Deployment

```bash
# 1. Run setup script
./setup.sh

# 2. Wait for services to start (2-3 minutes)

# 3. Access dashboard
open http://localhost:8083
```

### Verify Deployment

```bash
# Run integration tests
./integration-tests/test-pipeline.sh

# Check service health
curl http://localhost:8081/api/health  # Producer
curl http://localhost:8082/api/metrics/health  # Processor
curl http://localhost:8083/api/health  # Dashboard

# View logs
docker-compose logs -f stream-processor
```

## API Endpoints

### Stream Processor (`localhost:8082`)

```bash
# Current request counts (last 5 minutes)
GET /api/metrics/requests/current

# Detailed metrics for specific endpoint
GET /api/metrics/endpoint/{endpoint}

# Error counts by status code
GET /api/metrics/errors

# Regional distribution
GET /api/metrics/regions

# Health check
GET /api/metrics/health
```

### Dashboard API (`localhost:8083`)

```bash
# Endpoint-specific metrics
GET /api/metrics/endpoint/{endpoint}

# WebSocket connection
ws://localhost:8083/ws
```

## Load Testing

```bash
# Generate sustained load
./load-test.sh

# Monitor metrics
open http://localhost:9090  # Prometheus
open http://localhost:3000  # Grafana (admin/admin)
```

## Monitoring & Observability

### Prometheus Metrics
- `kafka_stream_consumer_fetch_manager_records_lag_max`: Consumer lag
- `kafka_stream_task_process_total`: Processing rate
- `kafka_stream_state_store_size`: State store size
- `rocksdb_total_sst_files_size`: RocksDB disk usage

### Grafana Dashboards
1. Navigate to http://localhost:3000
2. Login: admin/admin
3. Import dashboard from `monitoring/dashboards/`

### Key Metrics to Monitor
- **Stream processor lag**: Should be < 1000
- **Commit latency**: Target < 100ms
- **State store size**: Monitor growth rate
- **Rebalance frequency**: Should be rare

## Performance Characteristics

### Throughput
- **Log producer**: 40,000 events/second
- **Stream processor**: Processes all events in real-time
- **Interactive queries**: < 5ms latency
- **Dashboard updates**: 2-second polling interval

### Resource Usage
- **Stream processor**: ~2GB RAM, 20-30% CPU
- **State stores**: ~500MB/hour growth
- **Kafka**: 12 partitions, ~1GB disk/day

### Scalability
- Horizontal scaling: Add stream processor instances
- State partitioning: Automatic via consumer groups
- Interactive queries: Scatter-gather across instances

## Failure Scenarios

### Test Crash Recovery
```bash
# Kill stream processor
docker-compose kill stream-processor

# Observe: Dashboard shows stale data

# Restart processor
docker-compose up -d stream-processor

# Observe: State restoration from changelog (30-60s)
# Verify: No data loss, accurate counts
```

### Test Backpressure
```bash
# Increase producer rate 10x
# Edit log-producer/src/.../LogEventGenerator.java
# Change: @Scheduled(fixedRate = 25) -> fixedRate = 2

# Rebuild and restart
docker-compose up -d --build log-producer

# Monitor: Stream processor should pause consumption
# Check: kafka_stream_consumer_fetch_manager_records_lag_max
```

## Production Considerations

### State Store Management
- Configure compaction: `rocksdb.max-background-compactions`
- Monitor disk usage: `rocksdb.total-sst-files-size`
- Implement retention: Time-based window eviction

### Rebalancing
- Set generous timeout: `max.poll.interval.ms=300000` (5 min)
- Monitor: `kafka.consumer.group.rebalance.latency.total`
- Minimize: Keep processing fast, avoid external calls

### Interactive Queries
- Implement routing: Direct queries to correct partition
- Consider global stores: For frequently queried data
- Add caching: Redis for hot keys

### Exactly-Once vs At-Least-Once
- Use exactly-once for critical metrics
- Consider at-least-once for high-throughput monitoring
- Trade-off: 10-15% latency overhead

## Troubleshooting

### No Metrics Appearing
```bash
# Check Kafka connectivity
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Verify topic creation
docker-compose exec kafka kafka-topics --describe --topic log-events --bootstrap-server localhost:9092

# Check producer logs
docker-compose logs log-producer

# Verify stream processor state
curl http://localhost:8082/api/metrics/health
```

### High Consumer Lag
```bash
# Check processing rate
curl http://localhost:9090/api/v1/query?query=rate(kafka_stream_task_process_total[1m])

# Increase stream threads
# Edit: stream-processor/src/main/resources/application.yml
# Change: num.stream.threads: 4

# Scale horizontally
docker-compose up -d --scale stream-processor=2
```

### State Store Issues
```bash
# Check disk usage
du -sh ./stream-state

# Clear state (careful!)
docker-compose down
rm -rf ./stream-state/*
docker-compose up -d
```

## Extending the System

### Add New Aggregations
1. Edit `stream-processor/.../StreamsConfig.java`
2. Add new KStream transformation
3. Create corresponding state store
4. Expose via `InteractiveQueryController.java`

### Custom Windowing
```java
// Session windows (10-minute inactivity gap)
events.groupByKey()
    .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.ofMinutes(10)))
    .count()
```

### Anomaly Detection
```java
// Z-score calculation for outlier detection
events.mapValues(event -> {
    double zscore = (event.getResponseTimeMs() - mean) / stddev;
    return zscore > 3.0 ? "ANOMALY" : "NORMAL";
})
```

## Clean Up

```bash
# Stop all services
docker-compose down

# Remove volumes
docker-compose down -v

# Clean Maven build artifacts
mvn clean
```

## Learning Objectives

After completing this lesson, you should understand:

1. **Stream Processing**: How Kafka Streams processes unbounded data
2. **State Management**: RocksDB-backed stores with changelog topics
3. **Windowing**: Tumbling, hopping, session windows and their trade-offs
4. **Interactive Queries**: Serving real-time data from state stores
5. **Failure Recovery**: State restoration and exactly-once semantics

## Next Steps

- **Day 45**: Implement MapReduce for batch log analysis
- Combine real-time (streaming) with historical (batch) processing
- Learn lambda architecture patterns

## References

- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [RocksDB Tuning Guide](https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)

## License

This project is part of the 254-Day System Design Course.
For educational purposes only.
