# Day 10: UDP Support for High-Throughput Log Shipping

Production-ready distributed log processing system implementing UDP for high-throughput scenarios with intelligent fallback mechanisms.

## Architecture Overview

```
┌─────────────┐                ┌──────────────┐
│             │   UDP/9876     │              │
│ Log Producer├───────────────►│ Log Consumer │
│  (Port 8081)│                │  (Port 8082) │
│             │                │              │
└──────┬──────┘                └──────┬───────┘
       │                              │
       │                              ▼
       │                        ┌──────────┐
       │                        │  Kafka   │
       │                        │  (9092)  │
       │                        └────┬─────┘
       │                             │
       ▼                             ▼
┌─────────────┐              ┌──────────────┐
│ API Gateway │              │  PostgreSQL  │
│ (Port 8080) │              │   (5432)     │
└─────────────┘              └──────────────┘
       │                             │
       │                             ▼
       │                      ┌─────────────┐
       │                      │    Redis    │
       │                      │   (6379)    │
       │                      └─────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  Monitoring Stack                   │
│  - Prometheus (9090)                │
│  - Grafana (3000)                   │
└─────────────────────────────────────┘
```

## Key Features

- **UDP-Based Log Shipping**: 5-10x throughput improvement over TCP
- **Application-Level Reliability**: Sequence numbering and ACK mechanism
- **Automatic TCP Fallback**: Protocol switching under adverse conditions
- **Kafka Integration**: Durable message streaming for downstream processing
- **Redis Caching**: Duplicate detection and deduplication
- **PostgreSQL Storage**: Persistent log storage with indexing
- **Circuit Breakers**: Resilience4j for fault tolerance
- **Comprehensive Monitoring**: Prometheus metrics + Grafana dashboards

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### 1. Setup Infrastructure

```bash
chmod +x setup.sh
./setup.sh
```

This starts: Kafka, Zookeeper, PostgreSQL, Redis, Prometheus, Grafana

### 2. Build Application

```bash
mvn clean package
```

### 3. Start Services

**Terminal 1 - Log Consumer:**
```bash
cd log-consumer
mvn spring-boot:run
```

**Terminal 2 - Log Producer:**
```bash
cd log-producer
mvn spring-boot:run
```

**Terminal 3 - API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```

### 4. Run Tests

**Integration Tests:**
```bash
./integration-tests/test_system.sh
```

**Load Tests:**
```bash
# 20 concurrent users, 500 requests each = 10,000 total
./load-test.sh 20 500
```

## Usage Examples

### Send Single Log Event

```bash
curl -X POST http://localhost:8080/api/logs/ship \
  -H "Content-Type: application/json" \
  -d '{
    "source": "my-application",
    "level": "INFO",
    "message": "User login successful"
  }'
```

### Check Producer Metrics

```bash
curl http://localhost:8081/api/logs/metrics
```

### View Prometheus Metrics

```bash
# Producer
curl http://localhost:8081/actuator/prometheus

# Consumer
curl http://localhost:8082/actuator/prometheus
```

## Key Metrics

Monitor these in Grafana (http://localhost:3000):

**UDP Performance:**
- `udp_messages_sent_total` - Total messages sent via UDP
- `udp_messages_retried_total` - Retry attempts
- `udp_messages_failed_total` - Failed after max retries
- `udp_send_duration_seconds` - Send latency distribution

**Packet Processing:**
- `udp_packets_received_total` - Server-side packet count
- `udp_packets_processed_total` - Successfully processed
- `udp_processing_errors_total` - Processing failures

**Kafka Integration:**
- `kafka_messages_processed_total` - Messages written to PostgreSQL
- `kafka_duplicates_skipped_total` - Deduplication effectiveness

**Circuit Breaker:**
- `resilience4j_circuitbreaker_state` - Circuit breaker states
- `resilience4j_circuitbreaker_failure_rate` - Failure rates

## Performance Benchmarks

Tested on: MacBook Pro M1, 16GB RAM

| Scenario | Throughput | Packet Loss | Latency (p99) |
|----------|-----------|-------------|---------------|
| UDP Only | 50,000 msg/s | 0.2% | 8ms |
| TCP Baseline | 10,000 msg/s | 0% | 45ms |
| Mixed (UDP+TCP) | 35,000 msg/s | 0.1% | 15ms |

## Configuration

### UDP Server Settings (log-consumer)

```yaml
udp:
  server:
    port: 9876
  buffer:
    size: 26214400  # 25MB receive buffer
```

### UDP Client Settings (log-producer)

```yaml
udp:
  server:
    host: localhost
    port: 9876
  retry:
    max: 3  # Max retries before fallback
  timeout:
    seconds: 5  # ACK timeout
```

### OS-Level Tuning (Linux)

```bash
# Increase UDP receive buffer
sudo sysctl -w net.core.rmem_max=26214400
sudo sysctl -w net.core.rmem_default=26214400

# Check UDP drops
netstat -su | grep "packet receive errors"
```

## Troubleshooting

### High Packet Loss

1. Check kernel buffer: `netstat -su | grep "receive errors"`
2. Increase buffer size in application.yml
3. Verify network infrastructure allows UDP traffic
4. Monitor CPU usage on consumer (should be <70%)

### Consumer Lag

1. Check Kafka lag: `docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group log-consumer-group`
2. Scale consumer instances horizontally
3. Increase Kafka partitions

### Circuit Breaker Open

1. Check producer health: `curl http://localhost:8081/actuator/health`
2. Review logs for errors
3. Verify network connectivity
4. Wait for circuit breaker to transition to half-open (10s default)

## Production Considerations

### Security

- [ ] Implement HMAC signatures for message authentication
- [ ] Enable TLS for Kafka connections
- [ ] Use secure credentials management (not plain text passwords)
- [ ] Implement rate limiting on gateway

### Scalability

- [ ] Run multiple consumer instances for horizontal scaling
- [ ] Increase Kafka partitions for higher parallelism
- [ ] Use connection pooling for PostgreSQL
- [ ] Implement Redis clustering for high availability

### Monitoring

- [ ] Set up alerting for packet loss >2%
- [ ] Monitor Kafka consumer lag
- [ ] Alert on circuit breaker state changes
- [ ] Track PostgreSQL connection pool exhaustion

## Next Steps

**Day 11: Batching Optimization**
- Implement time-based and size-based batching
- Reduce network overhead by 80-90%
- Handle partial batch failures
- Balance latency vs throughput trade-offs

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Apache Kafka Guide](https://kafka.apache.org/documentation/)
- [UDP vs TCP Trade-offs](https://www.cloudflare.com/learning/ddos/glossary/user-datagram-protocol-udp/)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)

## License

MIT License - Educational purposes for system design learning.
