# Day 20: Log Format Compatibility Layer

A production-grade multi-protocol log ingestion and normalization system supporting syslog (RFC 3164/5424) and systemd journald.

## Architecture

The system consists of four main services:

1. **Syslog Adapter** (Port 8081)
   - UDP server on port 514 for traditional syslog
   - TCP server on port 601 for reliable syslog
   - Parses RFC 3164 and RFC 5424 formats
   - Produces to `raw-syslog-logs` topic

2. **Journald Adapter** (Port 8082)
   - Polls systemd journal via journalctl
   - Maintains cursor position in Redis
   - Produces to `raw-journald-logs` topic

3. **Format Normalizer** (Port 8083)
   - Consumes from raw topics
   - Normalizes to unified schema
   - Validates and enriches events
   - Produces to `normalized-logs` topic

4. **API Gateway** (Port 8080)
   - REST API for log queries
   - In-memory cache of recent logs
   - Statistics and health endpoints

## Quick Start

### 1. Generate the System

```bash
chmod +x generate_system_files.sh
./generate_system_files.sh
cd log-format-compatibility
```

### 2. Start Infrastructure

```bash
./setup.sh
```

This starts:
- Kafka (localhost:9092)
- Redis (localhost:6379)
- PostgreSQL (localhost:5432)
- Prometheus (localhost:9090)
- Grafana (localhost:3000, admin/admin)

### 3. Build Services

```bash
mvn clean install
```

### 4. Run Services

**Option 1: Start all services at once (recommended)**
```bash
./start-services.sh
```

This will start all services in the background. Logs are saved to the `logs/` directory.

**Option 2: Start services individually in separate terminals**
```bash
# Terminal 1: Syslog Adapter
java -jar syslog-adapter/target/syslog-adapter-1.0.0.jar

# Terminal 2: Journald Adapter (requires systemd)
java -jar journald-adapter/target/journald-adapter-1.0.0.jar

# Terminal 3: Format Normalizer
java -jar format-normalizer/target/format-normalizer-1.0.0.jar

# Terminal 4: API Gateway
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

**Stop all services:**
```bash
./stop-services.sh
```

### 5. Start Dashboard Server

```bash
cd dashboard-server
npm install
npm start
```

Access the dashboard at: http://localhost:8085

The dashboard provides:
- Real-time system metrics and health monitoring
- Integrated Prometheus query interface
- Embedded Grafana dashboards
- Log query and search interface
- Kafka topics visualization

## Testing

### Send Test Syslog Messages

RFC 3164 format (UDP):
```bash
echo "<34>Oct 11 22:14:15 mymachine su: Test message" | nc -u -w1 localhost 514
```

RFC 5424 format (TCP):
```bash
echo "<165>1 2024-01-01T12:00:00Z host app 1234 ID47 - Test message" | nc localhost 601
```

### Run Integration Tests

```bash
cd integration-tests
./test-syslog-ingestion.sh
./verify-pipeline.sh
```

### Load Testing

```bash
./load-test.sh
```

Generates 10,000 syslog messages and reports throughput.

## API Endpoints

### Query Logs
```bash
# Search all logs
curl "http://localhost:8080/api/logs/search?limit=100"

# Filter by level
curl "http://localhost:8080/api/logs/search?level=ERROR&limit=50"

# Filter by source
curl "http://localhost:8080/api/logs/search?source=syslog&limit=50"
```

### Get Statistics
```bash
curl "http://localhost:8080/api/logs/stats"
```

## Monitoring

### Dashboard Server

Access the comprehensive dashboard at http://localhost:8085

The dashboard provides:
- Real-time service health monitoring
- Live metrics visualization (ingestion rates, normalization rates, error rates, memory usage)
- Direct Prometheus query interface
- Embedded Grafana dashboards
- Log search and query interface
- Kafka topics overview

### Prometheus Metrics

Access at http://localhost:9090

Key queries:
- `rate(syslog_messages_produced_total[5m])` - Syslog ingestion rate
- `rate(normalizer_events_processed_total[5m])` - Normalization rate
- `normalizer_errors_total` - Normalization failures
- `jvm_memory_used_bytes` - Memory usage

### Grafana Dashboards

Access at http://localhost:3000 (admin/admin)

Create dashboards for:
- Message throughput per source
- Normalization success rate
- Error rates by service
- JVM metrics (memory, GC)

The dashboard server also provides embedded access to Grafana for seamless monitoring.

## System Design Patterns

### 1. Protocol Adapter Pattern
Separate adapters for each protocol (syslog, journald) isolate protocol handling from normalization logic.

### 2. Format Normalization
Unified schema with preserved raw format data allows consistent downstream processing while maintaining full fidelity.

### 3. Backpressure Handling
- UDP syslog: In-memory buffer with overflow to disk
- TCP syslog: Socket read pause when Kafka lags
- Journald: Query rate based on downstream capacity

### 4. Multi-Tenancy
Per-source rate limiting and isolation prevent noisy neighbors from impacting system.

### 5. Observability
Comprehensive metrics at each stage (ingestion, parsing, normalization) enable quick issue identification.

## Performance Characteristics

Expected throughput (per instance):
- Syslog Adapter: 45,000 events/sec
- Journald Adapter: 10,000 events/sec
- Format Normalizer: 50,000 events/sec
- API Gateway: 100,000 queries/sec

Memory requirements:
- Syslog Adapter: 1-2GB
- Journald Adapter: 512MB-1GB
- Format Normalizer: 1-2GB
- API Gateway: 2-4GB (depends on cache size)

## Scaling Strategy

### Horizontal Scaling
- Deploy multiple instances of each service
- Kafka partitioning provides natural load distribution
- Redis cluster for shared state
- PostgreSQL read replicas for query scaling

### Vertical Scaling
- Increase JVM heap for larger in-memory buffers
- More CPU cores for parsing parallelism
- Faster disks for overflow buffering

## Production Considerations

### Security
- TLS for TCP syslog connections
- mTLS for Kafka connections
- Authentication for API endpoints
- Input validation and sanitization

### Reliability
- Circuit breakers for downstream dependencies
- Retry logic with exponential backoff
- Dead letter queues for failed messages
- Regular backup of Redis cursor data

### Operations
- Automated deployment with Kubernetes
- Blue-green deployments for zero downtime
- Centralized logging for troubleshooting
- Alerting on error rates and latency

## Troubleshooting

### No messages appearing in Kafka
1. Check adapter health: `curl http://localhost:8081/actuator/health`
2. Verify Kafka connection: Check adapter logs for connection errors
3. Test direct Kafka: `kafka-console-consumer --topic raw-syslog-logs`

### High normalization errors
1. Check schema compatibility
2. Review error logs in format-normalizer
3. Validate source message formats

### Memory issues
1. Reduce buffer sizes in configuration
2. Increase JVM heap: `-Xmx4g`
3. Enable GC logging: `-XX:+PrintGCDetails`

## Next Steps: Day 21

Tomorrow we'll build a log enrichment pipeline that:
- Adds hostname resolution
- Performs IP geolocation
- Tags with environment metadata
- Adds correlation IDs for tracing

The enrichment layer builds on this normalized format, adding intelligence without modifying core adapters.

## Connection to Enterprise Scale

This pattern scales to Netflix/Uber/Amazon levels:
- Netflix: 2+ billion events/day from 50,000+ instances
- Uber: 200,000+ containers with adaptive rate limiting
- Amazon: Petabytes of logs with multi-region replication

The same adapter patterns, normalization approach, and backpressure mechanisms work at any scale through horizontal replication.
