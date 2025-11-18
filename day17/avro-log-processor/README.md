# Avro Log Processing System with Schema Evolution

A production-ready distributed log processing system demonstrating Apache Avro serialization with schema evolution support.

## Architecture

- **API Gateway** (port 8080): Entry point for all requests
- **Log Producer** (port 8081): Produces Avro-serialized log events to Kafka
- **Log Consumer** (port 8082): Consumes and processes log events with schema resolution
- **Schema Registry** (port 8085): Centralized schema management
- **Kafka** (port 9092): Message streaming
- **Redis** (port 6379): Event caching and indexing
- **Prometheus** (port 9090): Metrics collection
- **Grafana** (port 3000): Metrics visualization

## Quick Start

1. **Start infrastructure:**
   ```bash
   ./setup.sh
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Start services** (in separate terminals):
   ```bash
   java -jar log-producer/target/log-producer-1.0.0.jar
   java -jar log-consumer/target/log-consumer-1.0.0.jar
   java -jar api-gateway/target/api-gateway-1.0.0.jar
   ```

4. **Run the schema evolution demo:**
   ```bash
   ./demo-schema-evolution.sh
   ```

## Schema Evolution

This system demonstrates Avro's schema evolution capabilities:

### V1 Schema (Baseline)
- id, timestamp, level, message, source, schemaVersion

### V2 Schema (Evolved)
- All V1 fields
- correlationId (optional) - for distributed tracing
- tags (map) - flexible metadata
- spanId, parentSpanId (optional) - trace context

V2 is backward compatible with V1, meaning:
- V2 consumers can read V1 events (new fields get defaults)
- V1 consumers can read V2 events (new fields ignored)

## API Endpoints

### Produce Events
```bash
# V1 event
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{"level":"INFO","message":"Test","source":"app","schemaVersion":1}'

# V2 event with tracing
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level":"INFO",
    "message":"Test",
    "source":"app",
    "correlationId":"abc-123",
    "tags":{"env":"prod"},
    "schemaVersion":2
  }'
```

### Query Events
```bash
# By event ID
curl http://localhost:8080/api/logs/{eventId}

# By correlation ID
curl http://localhost:8080/api/correlation/{correlationId}
```

### Schema Registry
```bash
# List subjects
curl http://localhost:8085/subjects

# Get schema versions
curl http://localhost:8085/subjects/avro-log-events-value/versions
```

## Load Testing

```bash
./load-test.sh
```

View results in Grafana at http://localhost:3000 (admin/admin)

## Integration Tests

```bash
./integration-tests/test-system.sh
```

## Monitoring

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

Key metrics:
- `avro_events_produced_total` - Total events produced
- `avro_events_consumed_total` - Total events consumed
- `avro_events_v1_total` / `avro_events_v2_total` - Events by schema version
- `avro_processing_time_seconds` - Processing latency

## Cleanup

```bash
docker compose down -v
```
