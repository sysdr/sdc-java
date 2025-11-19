# Day 18: Log Format Normalization Service

A production-grade service for transforming logs between different serialization formats: TEXT, JSON, Protocol Buffers, and Avro.

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│ Log Sources │────▶│ Format       │────▶│ Normalizer      │
│ (Mixed      │     │ Detector     │     │ Service         │
│  Formats)   │     └──────────────┘     │                 │
└─────────────┘                          │ ┌─────────────┐ │
                                         │ │ Canonical   │ │
                                         │ │ Form        │ │
                                         │ └─────────────┘ │
                                         │        │        │
                                         │        ▼        │
                                         │ ┌─────────────┐ │
                                         │ │ Target      │ │
                                         │ │ Serializer  │ │
                                         │ └─────────────┘ │
                                         └────────┬────────┘
                                                  │
                                                  ▼
                                         ┌─────────────────┐
                                         │ Output Topics   │
                                         │ (Per Format)    │
                                         └─────────────────┘
```

## Quick Start

```bash
# Build and start all services
./setup.sh

# Run integration tests
./integration-tests/test-normalization.sh

# Run load tests
./load-test.sh
```

## API Endpoints

### Normalize Single Log
```bash
# JSON to AVRO
curl -X POST "http://localhost:8081/api/v1/normalize/json?targetFormat=AVRO" \
    -H "Content-Type: application/json" \
    -d '{"level":"INFO","message":"test","service":"my-svc","host":"localhost"}'

# TEXT to JSON
curl -X POST "http://localhost:8081/api/v1/normalize/text?targetFormat=JSON" \
    -H "Content-Type: text/plain" \
    -d '2024-01-01T12:00:00Z [INFO] [service] Message'
```

### Batch Normalization
```bash
curl -X POST "http://localhost:8081/api/v1/normalize/batch" \
    -H "Content-Type: application/json" \
    -d '{
        "logs": [
            {"id": "1", "data": "<base64>", "format": "JSON"},
            {"id": "2", "data": "<base64>", "format": "TEXT"}
        ],
        "targetFormat": "AVRO"
    }'
```

### Generate Test Logs
```bash
curl -X POST "http://localhost:8082/api/v1/producer/send?count=100&format=JSON"
```

## Supported Formats

| Format | Content Type | Description |
|--------|-------------|-------------|
| TEXT | text/plain | Structured text logs |
| JSON | application/json | JSON objects |
| PROTOBUF | application/x-protobuf | Protocol Buffers binary |
| AVRO | application/avro | Apache Avro binary |

## Monitoring

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

### Key Metrics

- `normalization_success_total` - Successful conversions by format pair
- `normalization_failure_total` - Failed conversions by format pair
- `normalization_duration_seconds` - Conversion latency histogram
- `format_detection_total` - Format detection attempts

## Services

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Unified entry point |
| Normalizer | 8081 | Format transformation |
| Producer | 8082 | Test log generation |
| Kafka | 9092 | Message streaming |
| Redis | 6379 | Caching |

## Performance

Expected throughput with optimizations:
- Single conversion: ~50,000 logs/second per core
- Batch processing: Higher throughput with reduced overhead
- Passthrough (same format): Near wire speed

## Next Steps

Day 19 will add a Schema Registry service for centralized format management and validation.
