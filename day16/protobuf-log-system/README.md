# Protocol Buffers Log Processing System

A production-ready distributed log processing system demonstrating efficient binary serialization with Protocol Buffers.

## Architecture

```
[Producer API]    [Protobuf Messages]    [Consumer]    [Redis Cache]
     |                    |                   |              |
     +---> Kafka Topic ---+---> Deserialize --+---> Store ---+
     |                                                        |
     JSON/Binary                                              |
     Endpoints                                    [API Gateway] <-- Query
```

### Key Components

- **log-producer**: HTTP API accepting JSON and binary protobuf formats
- **log-consumer**: Kafka consumer processing protobuf messages
- **api-gateway**: Query interface for stored logs
- **Infrastructure**: Kafka, Redis, PostgreSQL, Prometheus, Grafana

## Performance Benefits

Protocol Buffers provides:
- **60-80% smaller message size** vs JSON
- **40-60% higher throughput** due to efficient serialization
- **Type safety** with schema-driven code generation
- **Forward/backward compatibility** for zero-downtime deployments

## Quick Start

### 1. Generate All Files

```bash
chmod +x setup.sh
./setup.sh
cd protobuf-log-system
```

### 2. Start Infrastructure

```bash
./setup.sh
```

Wait ~30 seconds for services to initialize.

### 3. Build Services

```bash
mvn clean install
```

### 4. Start Applications

```bash
# Terminal 1: Producer
java -jar log-producer/target/log-producer-1.0.0.jar

# Terminal 2: Consumer
java -jar log-consumer/target/log-consumer-1.0.0.jar

# Terminal 3: Gateway
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

## Testing

### Send JSON Log Event

```bash
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "timestamp": "2024-01-15T10:30:00Z",
    "level": "INFO",
    "message": "User login successful",
    "serviceName": "auth-service",
    "environment": "production",
    "tags": {"user_id": "12345", "ip": "192.168.1.1"}
  }'
```

### Send Binary Protobuf Event

First, create a protobuf message using your client library, then:

```bash
# Example using protoc to encode
echo '
event_id: "evt-002"
timestamp: "2024-01-15T10:31:00Z"
level: INFO
message: "Payment processed"
service_name: "payment-service"
' | protoc --encode=LogEvent log_event.proto > event.bin

curl -X POST http://localhost:8081/api/logs/binary \
  -H "Content-Type: application/x-protobuf" \
  --data-binary @event.bin
```

### Query Logs

```bash
# Get recent logs
curl http://localhost:8080/api/query/recent?limit=10

# Get specific log
curl http://localhost:8080/api/query/event/evt-001

# Get statistics
curl http://localhost:8080/api/query/stats
```

### Run Load Tests

```bash
./load-test.sh
```

### Run Integration Tests

```bash
./integration-tests/test-protobuf-flow.sh
```

## Monitoring

### Prometheus Metrics

Access: http://localhost:9090

Key metrics:
```
# Message throughput by format
rate(log_events_json_total[1m])
rate(log_events_protobuf_total[1m])

# Processing latency
histogram_quantile(0.99, log_events_json_processing_seconds)
histogram_quantile(0.99, log_events_protobuf_processing_seconds)

# Kafka metrics
rate(kafka_publish_success[1m])
kafka_publish_failure
```

### Grafana Dashboards

Access: http://localhost:3000 (admin/admin)

Pre-configured dashboard: **Protocol Buffers Performance**

## Performance Analysis

### Expected Results

| Metric | JSON | Protobuf | Improvement |
|--------|------|----------|-------------|
| Message Size | 350 bytes | 120 bytes | 65% reduction |
| Throughput | 2,000 req/s | 3,200 req/s | 60% increase |
| P99 Latency | 45ms | 28ms | 38% faster |
| CPU Usage | 65% | 40% | 38% reduction |

### Measurement Strategy

1. **Baseline**: Run load test with JSON only
2. **Protobuf**: Run identical test with binary format
3. **Compare**: Analyze Prometheus metrics for:
   - Throughput (requests/sec)
   - Latency percentiles (P50, P95, P99)
   - CPU utilization
   - Network bandwidth

## Production Considerations

### Schema Evolution

When updating `log_event.proto`:

1. **Never** reuse field numbers
2. **Add** new optional fields with new numbers
3. **Deprecate** fields with comments before removal
4. **Test** compatibility with old consumers

Example safe evolution:
```protobuf
message LogEvent {
  string event_id = 1;
  string timestamp = 2;
  // ... existing fields ...
  
  // New field - safe to add
  string user_id = 30;
  
  // Deprecated - keep for compatibility
  string old_field = 15 [deprecated = true];
}
```

### Deployment Strategy

1. Deploy updated **consumers** first (handle new fields)
2. Deploy **producers** with new schema
3. Monitor for deserialization errors
4. Remove deprecated field readers after migration window

### Error Handling

The consumer implements:
- **Deserialization errors**: Logged and skipped (consider DLQ)
- **Processing failures**: Retried with exponential backoff
- **Schema mismatches**: Caught and alerted via metrics

### Debugging Binary Messages

```bash
# Decode protobuf message from Kafka
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic log-events-protobuf \
  --property print.key=true \
  --max-messages 1 | \
  protoc --decode=LogEvent log_event.proto
```

## Troubleshooting

### Kafka Connection Issues

```bash
# Check Kafka is ready
docker logs $(docker ps -qf "name=kafka")

# Verify topic exists
docker exec -it $(docker ps -qf "name=kafka") \
  kafka-topics --list --bootstrap-server localhost:9092
```

### Protobuf Compilation Errors

```bash
# Regenerate protobuf classes
mvn clean compile -pl log-producer,log-consumer
```

### Consumer Not Processing Messages

```bash
# Check consumer group lag
docker exec -it $(docker ps -qf "name=kafka") \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group log-consumer-group
```

## Architecture Decisions

### Why Protobuf Over JSON?

- **Performance**: 3-5x better throughput at scale
- **Type Safety**: Compile-time validation prevents runtime errors
- **Versioning**: Built-in schema evolution support
- **Efficiency**: Smaller messages reduce network costs

### When to Use Each Format

- **JSON**: Public APIs, debugging, human-readable logs
- **Protobuf**: Internal services, high-throughput paths, data pipelines

### Dual Format Strategy

This system supports both:
- Maintains JSON for compatibility
- Uses protobuf for Kafka (internal communication)
- Enables gradual migration path

## Scaling Considerations

### Horizontal Scaling

- Producer: Stateless, scale to N instances behind load balancer
- Consumer: Add more consumer instances (up to # of Kafka partitions)
- Gateway: Stateless, scale independently

### Kafka Partitioning

Current: 3 partitions
- Scale to 10-20 for higher throughput
- Use consistent hashing on event_id for ordering

### Redis Clustering

For production:
- Use Redis Cluster for horizontal scaling
- Implement connection pooling
- Add read replicas for query workloads

## Next Steps

- **Day 17**: Implement Apache Avro with schema registry
- **Day 18**: Add gRPC endpoints using protobuf
- **Day 19**: Implement schema versioning strategies
- **Day 20**: Performance tuning and capacity planning

## References

- Protocol Buffers Guide: https://protobuf.dev/
- Spring Kafka: https://spring.io/projects/spring-kafka
- Micrometer Metrics: https://micrometer.io/
