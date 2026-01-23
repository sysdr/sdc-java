# Day 47: Sliding Window Analytics System

## System Architecture

This system implements real-time trend analysis using sliding/hopping windows in Kafka Streams:

- **Log Producer**: Generates synthetic log events (50 events/sec)
- **Stream Processor**: Processes events with multiple window sizes (1min, 5min, 15min)
- **Query API**: Serves real-time trends with Redis caching
- **Infrastructure**: Kafka, Redis, Prometheus, Grafana

## Key Features

1. **Multi-granularity Windows**: 1-minute, 5-minute, and 15-minute hopping windows
2. **Hopping Windows**: 10-second hops for continuous trend updates
3. **Efficient State Management**: Kafka Streams + RocksDB
4. **Query Caching**: Redis caching with 10-second TTL
5. **Production Monitoring**: Prometheus metrics + Grafana dashboards

## Quick Start

### 1. Start Infrastructure
```bash
./setup.sh
```

### 2. Build All Services
```bash
# From project root
mvn clean package
```

### 3. Run Services (in separate terminals)
```bash
# Terminal 1: Log Producer
cd log-producer && mvn spring-boot:run

# Terminal 2: Stream Processor
cd stream-processor && mvn spring-boot:run

# Terminal 3: Query API
cd query-api && mvn spring-boot:run
```

### 4. Test the System
```bash
# Query trends for a service
curl http://localhost:8083/api/v1/trends/api-gateway | jq

# Run integration tests
./integration-tests/test-trends.sh

# Run load test
./load-test.sh
```

## API Endpoints

### Query Trends
```bash
GET /api/v1/trends/{serviceId}

Response:
{
  "service_id": "api-gateway",
  "one_min_avg_latency": 52.3,
  "five_min_avg_latency": 51.8,
  "fifteen_min_avg_latency": 52.1,
  "one_min_avg_error_rate": 0.012,
  "five_min_avg_error_rate": 0.011,
  "fifteen_min_avg_error_rate": 0.010,
  "one_min_throughput": 450,
  "five_min_throughput": 445,
  "fifteen_min_throughput": 448,
  "timestamp": 1704123456789,
  "from_cache": false
}
```

## Monitoring

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

Key metrics:
- `log_events_produced_total`: Events produced rate
- `stream_events_processed_total`: Events processed rate
- `query_cache_hit_total`: Cache hit rate
- `query_latency_seconds`: Query latency distribution

## Performance Characteristics

- **Throughput**: 50 events/sec (250 events/sec across 5 services)
- **Window Sizes**: 1min, 5min, 15min with 10-second hops
- **Query Latency**: <10ms (cached), <50ms (uncached)
- **State Store Size**: ~30MB per window size
- **Cache Hit Rate**: >90% with 10-second TTL

## Architecture Decisions

### Window Configuration
- **Hopping windows** instead of sliding windows for efficiency
- **10-second hop interval** balances freshness vs computational cost
- **30-second grace period** handles typical network delays

### State Management
- **RocksDB state stores** for windowed aggregations
- **Incremental aggregation** (sum/count) for O(1) updates
- **Partition-local state** eliminates distributed coordination

### Query Pattern
- **Redis caching** with 10-second TTL (matches hop interval)
- **Interactive queries** directly from Kafka Streams state stores
- **Local queries** avoid network overhead

## Troubleshooting

### Services won't start
- Check Docker containers: `docker-compose ps`
- Check Kafka logs: `docker-compose logs kafka`
- Ensure ports 8081-8083, 9090, 3000 are available

### No data in windows
- Verify log producer is running and producing events
- Check Kafka topic has data: `docker exec -it $(docker ps -q -f name=kafka) kafka-console-consumer --bootstrap-server localhost:9092 --topic log-events --from-beginning --max-messages 10`
- Check stream processor logs for errors

### High query latency
- Check Redis is running: `redis-cli ping`
- Monitor cache hit rate in Grafana
- Verify state stores are built: check stream processor logs

## Production Considerations

1. **Scaling**: Run multiple stream processor instances for horizontal scaling
2. **State Store Size**: Monitor with `kafka.streams.state.store.bytes.total`
3. **Grace Period**: Tune based on p95 network latency
4. **Retention**: Adjust window retention based on query patterns
5. **Monitoring**: Alert on high lag, low cache hit rate, high query latency

## Clean Up
```bash
docker-compose down -v
rm -rf /tmp/kafka-streams*
```
