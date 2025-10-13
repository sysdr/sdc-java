# Day 12: Compressed Log Processing System

A production-ready distributed log processing system demonstrating compression techniques to reduce network bandwidth usage.

## Architecture

```
Client → API Gateway → Log Producer → Kafka → Log Consumer → PostgreSQL
                            ↓                        ↓
                        Compression              Decompression
                            ↓                        ↓
                        Prometheus ← Metrics ← Redis Cache
```

## Features

- **Multi-Algorithm Compression**: Gzip, Snappy, LZ4 with adaptive selection
- **Bandwidth Telemetry**: Real-time compression ratio and throughput metrics
- **Circuit Breaker Protection**: Resilient decompression with failure handling
- **Distributed Tracing**: End-to-end observability with Prometheus/Grafana
- **Production-Ready**: Docker containerization, health checks, integration tests

## Quick Start

### 1. Generate and Setup Infrastructure

```bash
chmod +x generate_system_files.sh
./generate_system_files.sh
cd compressed-log-system
./setup.sh
```

### 2. Build Services

```bash
mvn clean package
```

### 3. Run Services

```bash
# Terminal 1: API Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 2: Log Producer
cd log-producer && mvn spring-boot:run

# Terminal 3: Log Consumer
cd log-consumer && mvn spring-boot:run
```

### 4. Verify System

```bash
# Send test log
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "service": "test",
    "message": "Hello compressed logs!",
    "user_id": "user123"
  }'

# Check metrics
curl http://localhost:8081/actuator/prometheus | grep compression
```

### 5. Run Load Test

```bash
./load-test.sh
```

### 6. View Dashboards

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
  - Import dashboard from `monitoring/dashboards/compression-dashboard.json`

## Compression Algorithms

| Algorithm | Ratio | CPU/MB | Best For |
|-----------|-------|--------|----------|
| **Gzip** | 70-80% | 20-30ms | Batch processing, high compression |
| **Snappy** | 50-60% | 1-2ms | Real-time streams, balanced performance |
| **LZ4** | 40-55% | 0.5-1ms | Ultra-low latency, fast decompression |

## Key Metrics

- `compression_ratio`: Compression effectiveness by algorithm
- `compression_bytes_saved_total`: Cumulative bandwidth saved
- `compression_latency`: Compression overhead (P50, P99)
- `decompression_failures_total`: Circuit breaker triggers

## Performance Benchmarks

With 10,000 events/second:
- Uncompressed: 1 Gbps network utilization
- Snappy: 350 Mbps (65% reduction, 5% CPU overhead)
- LZ4: 500 Mbps (50% reduction, 2% CPU overhead)

## Testing

```bash
# Integration tests
./integration-tests/test-compression.sh

# Unit tests
mvn test
```

## Production Considerations

### Monitoring Alerts

Set alerts for:
- Compression ratio < 30% (data pattern change)
- Decompression latency P99 > 10ms (CPU saturation)
- Circuit breaker opens (systematic failures)

### Scaling Strategy

- **Horizontal**: Add producer/consumer instances
- **Vertical**: Increase Kafka partitions for parallelism
- **Optimization**: Tune batch size vs compression ratio

### Failure Scenarios

1. **Corrupted payload**: Circuit breaker → DLQ → Alert
2. **CPU saturation**: Adaptive algorithm selection → Fallback to lighter compression
3. **Network partition**: Kafka replication ensures no data loss

## Architecture Decisions

### Why Compression at Producer?

- Pushes CPU cost to horizontally scalable producers
- Reduces broker storage and network load
- Consumers decompress in parallel

### Why Multiple Algorithms?

- Different log patterns compress differently
- Trade-off between ratio and latency varies by use case
- Adaptive selection optimizes for actual workload

### Why Circuit Breaker?

- Prevents cascading failures on corrupt data
- Maintains system availability over strict consistency
- Provides graceful degradation

## Next Steps

Tomorrow: **TLS Encryption for Secure Log Transmission**

Challenge: Measure your compression effectiveness and identify opportunities for optimization.

## Troubleshooting

### Services won't start
```bash
docker compose logs
```

### Kafka connection refused
Wait 30 seconds after `docker compose up` for Kafka initialization

### Out of memory
Increase Docker memory allocation to 4GB+

## Resources

- [Compression Algorithms Comparison](https://github.com/google/snappy)
- [Kafka Compression Guide](https://kafka.apache.org/documentation/#compression)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
