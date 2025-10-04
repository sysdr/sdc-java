# TCP Log Server - Day 8 System Implementation

A production-ready TCP log server built with Spring Boot, demonstrating distributed system patterns for network-based log collection.

## Architecture Overview

```
┌─────────────┐
│   Clients   │ (Multiple TCP connections)
└──────┬──────┘
       │ TCP:9090
       ▼
┌─────────────────────────────────────────┐
│     TCP Log Server (Netty)              │
│  ┌────────────────────────────────┐     │
│  │  Connection Handler             │     │
│  │  - Non-blocking I/O             │     │
│  │  - Line-based framing           │     │
│  │  - Connection limits            │     │
│  └─────────────┬──────────────────┘     │
│                ▼                         │
│  ┌────────────────────────────────┐     │
│  │  Log Buffer Service             │     │
│  │  - Bounded queue (10K msgs)     │     │
│  │  - Batch writes (1000 msgs)     │     │
│  │  - Circuit breaker              │     │
│  └─────────────┬──────────────────┘     │
└────────────────┼────────────────────────┘
                 ▼
        ┌────────────────┐
        │   PostgreSQL   │
        │   (JSONB logs) │
        └────────────────┘
```

## Features

- **Non-blocking TCP Server**: Handles 1000+ concurrent connections using Netty
- **Backpressure Management**: Bounded buffers prevent memory exhaustion
- **Batch Processing**: Writes 1000 logs per batch for optimal throughput
- **Circuit Breaker**: Protects database from cascading failures
- **Comprehensive Metrics**: Prometheus integration with Grafana dashboards
- **Graceful Shutdown**: Flushes buffered logs before termination
- **REST API**: Query logs and monitor server health

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- curl and netcat (for testing)

### Setup

```bash
# Generate and setup the system
chmod +x setup.sh
./setup.sh
```

This will:
1. Build the log-server application
2. Start PostgreSQL, Prometheus, and Grafana
3. Deploy the TCP log server
4. Wait for all services to be healthy

### Verify Installation

```bash
# Check health
curl http://localhost:8080/actuator/health

# View metrics
curl http://localhost:8080/actuator/prometheus
```

## Usage

### Send Logs via TCP

```bash
# Single log message
echo '{"timestamp":"2025-01-15T10:30:00Z","level":"INFO","message":"Hello from TCP","source":"test"}' | nc localhost 9090

# Multiple logs
for i in {1..100}; do
  echo "{\"timestamp\":\"2025-01-15T10:30:00Z\",\"level\":\"INFO\",\"message\":\"Message $i\",\"source\":\"batch-test\"}" | nc localhost 9090
done
```

### Query Logs via REST API

```bash
# Search logs by level and time range
curl "http://localhost:8080/api/logs/search?level=INFO&start=2025-01-15T10:00:00Z&end=2025-01-15T12:00:00Z"

# Get logs by source
curl "http://localhost:8080/api/logs/source/test"

# Get statistics
curl "http://localhost:8080/api/logs/stats?start=2025-01-15T10:00:00Z&end=2025-01-15T12:00:00Z"
```

## Testing

### Integration Tests

```bash
./integration-tests/test-tcp-connection.sh
```

This sends test messages and verifies:
- TCP connection establishment
- Message parsing and storage
- REST API queries
- Metrics collection

### Load Testing

```bash
./load-test.sh
```

Generates sustained load:
- 100 messages/second for 60 seconds
- Total: 6000 messages
- Validates throughput and buffering

## Monitoring

### Prometheus

Access Prometheus at http://localhost:9091

Key metrics:
- `tcp_active_connections`: Current TCP connections
- `log_messages_received_total`: Total messages received
- `log_messages_dropped_total`: Messages dropped (buffer full)
- `db_batch_write_duration`: Database write latency

### Grafana

Access Grafana at http://localhost:3000 (admin/admin)

Pre-configured dashboard shows:
- Active connections over time
- Message receive rate
- Drop rate (should be 0 under normal load)
- P99 database write latency

## Configuration

Edit `log-server/src/main/resources/application.yml`:

```yaml
tcp:
  server:
    port: 9090                    # TCP server port
    max-connections: 1000          # Max concurrent connections
    idle-timeout-seconds: 30       # Close idle connections

buffer:
  max-size: 10000                  # Max buffered messages
  batch-size: 1000                 # Batch write size
```

## Production Considerations

### Capacity Planning

- **Memory**: ~100 bytes per buffered message
- **CPU**: 1 core handles ~10K msg/sec
- **Database**: PostgreSQL write throughput ~10K inserts/sec
- **Network**: 1 Gbps handles ~100K small messages/sec

### Scaling Strategies

**Vertical Scaling:**
- Increase buffer size for traffic spikes
- Add CPU cores for higher throughput
- Use connection pooling for database

**Horizontal Scaling:**
- Deploy multiple instances behind load balancer
- Use sticky sessions or stateless design
- Shard PostgreSQL by timestamp or source

### Failure Scenarios

| Scenario | Behavior | Recovery |
|----------|----------|----------|
| Database down | Circuit breaker opens, logs buffer in memory | Auto-reconnect after 30s |
| Buffer full | Drop oldest messages, increment counter | Scale up or increase flush rate |
| Network partition | Clients reconnect with backoff | Connection re-established |
| OOM | Service crashes, loses buffered logs | Restart, reduce buffer size |

### Monitoring Alerts

Set up alerts for:
- Active connections > 800 (approaching limit)
- Buffer depth > 8000 (approaching limit)
- Drop rate > 0 (losing data)
- Write latency P99 > 100ms (database slowdown)
- Circuit breaker open (database unavailable)

## Architecture Decisions

### Why Netty?

- Non-blocking I/O scales to 10K+ connections
- Mature, battle-tested in production systems
- Spring Boot integration reduces complexity

### Why Line-Delimited JSON?

- Human-readable for debugging
- Compatible with existing tools (Logstash, Fluentd)
- Simple framing (newline delimiter)

### Why Batch Writes?

- Single insert: 100 TPS
- Batch of 1000: 10K TPS
- Trade-off: Small latency increase (<5s) for 100x throughput

### Why Circuit Breaker?

- Prevents cascading failures
- Fails fast when database unavailable
- Automatic recovery reduces manual intervention

## Troubleshooting

### "Connection refused" on port 9090

Check if server is running:
```bash
docker-compose ps
docker logs log-server
```

### High drop rate

Increase buffer size or batch frequency:
```yaml
buffer:
  max-size: 20000    # Double the buffer
  batch-size: 500    # Flush more frequently
```

### Database write failures

Check PostgreSQL connection:
```bash
docker-compose logs postgres
curl http://localhost:8080/actuator/health
```

## Next Steps

Tomorrow (Day 9), you'll build the **log shipping client** that:
- Reads logs from local files
- Forwards to this TCP server
- Handles connection failures with retry
- Implements client-side buffering

This completes the producer-consumer pattern for distributed log collection.

## License

MIT License - See LICENSE file for details
