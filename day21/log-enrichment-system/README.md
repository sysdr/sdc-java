# Day 21: Log Enrichment Pipeline

A production-grade distributed log enrichment system that adds contextual metadata to raw log events using Spring Boot, Kafka Streams, Redis, and PostgreSQL.

## Architecture Overview

```
Raw Logs → Kafka → Enrichment Service → Enriched Logs
                         ↓
                   [Redis Cache]
                   [PostgreSQL]
                   [Circuit Breakers]
```

### Components

1. **Log Producer** (Port 8080)
   - REST API for ingesting logs
   - Publishes to Kafka `raw-logs` topic

2. **Enrichment Service** (Port 8081)
   - Kafka Streams topology for real-time enrichment
   - Multi-tier metadata resolution (Redis → PostgreSQL)
   - Circuit breaker protection with Resilience4j
   - Produces to `enriched-logs-complete` and `enriched-logs-partial` topics

3. **Metadata Service** (Port 8082)
   - Manages host and service metadata
   - Provides REST API for metadata CRUD operations

4. **Infrastructure**
   - Kafka + Zookeeper for event streaming
   - Redis for distributed caching
   - PostgreSQL for metadata storage
   - Prometheus for metrics collection
   - Grafana for visualization

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Setup

1. **Generate and navigate to project**
   ```bash
   ./generate_system_files.sh
   cd log-enrichment-system
   ```

2. **Start infrastructure**
   ```bash
   ./setup.sh
   ```

3. **Build services**
   ```bash
   mvn clean package
   ```

4. **Start services** (in separate terminals)
   ```bash
   # Terminal 1: Log Producer
   java -jar log-producer/target/log-producer-1.0.0.jar

   # Terminal 2: Metadata Service
   java -jar metadata-service/target/metadata-service-1.0.0.jar

   # Terminal 3: Enrichment Service
   java -jar enrichment-service/target/enrichment-service-1.0.0.jar
   ```

## Usage Examples

### Send a log event
```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR",
    "message": "Payment processing failed",
    "service": "payment-service",
    "source_ip": "192.168.1.100"
  }'
```

### View enriched logs
```bash
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic enriched-logs-complete \
  --from-beginning
```

### Add host metadata
```bash
curl -X POST http://localhost:8082/api/metadata/hosts \
  -H "Content-Type: application/json" \
  -d '{
    "hostname": "new-server",
    "ipAddress": "192.168.1.150",
    "datacenter": "us-west-2",
    "environment": "production",
    "costCenter": "engineering"
  }'
```

## Load Testing

Run the load test to generate 100 logs/second for 60 seconds:

```bash
./load-test.sh
```

Expected performance:
- **Throughput**: 10,000+ logs/second per instance
- **Latency P99**: < 10ms for enrichment
- **Success Rate**: > 99.9%

## Monitoring

### Prometheus Metrics
Access Prometheus at http://localhost:9090

Key queries:
```promql
# Enrichment throughput
rate(enrichment_attempts_total[1m])

# Success rate
rate(enrichment_successes_total[1m]) / rate(enrichment_attempts_total[1m])

# P99 latency
histogram_quantile(0.99, rate(enrichment_latency_seconds_bucket[5m]))

# Circuit breaker state
resilience4j_circuitbreaker_state
```

### Grafana Dashboards
Access Grafana at http://localhost:3000 (admin/admin)

Create dashboards for:
- Enrichment throughput and latency
- Coverage percentage trends
- Circuit breaker states
- Redis cache hit rates

## Testing Failure Scenarios

### Scenario 1: Redis Failure
```bash
# Stop Redis
docker-compose stop redis

# Observe circuit breaker opening
curl http://localhost:8081/actuator/health

# Logs continue flowing with partial enrichment
./load-test.sh

# Restart Redis
docker-compose start redis
```

### Scenario 2: PostgreSQL Failure
```bash
# Stop PostgreSQL
docker-compose stop postgres

# Circuit breaker opens for database lookups
# Service continues with cache-only enrichment
```

### Scenario 3: High Load
```bash
# Increase load to 500 logs/second
RATE=500 ./load-test.sh

# Monitor latency increase in Prometheus
# Verify backpressure handling
```

## Architecture Patterns

### 1. Staged Enrichment Pipeline
- **Stage 1**: Filter invalid logs
- **Stage 2**: Metadata lookup with circuit breakers
- **Stage 3**: Merge and validate enrichment
- **Stage 4**: Branch into complete/partial topics

### 2. Tiered Caching Strategy
- **Tier 1**: Kafka Streams state store (sub-ms)
- **Tier 2**: Redis distributed cache (1-2ms)
- **Tier 3**: PostgreSQL (5-10ms)

### 3. Circuit Breaker Protection
Each metadata source has independent circuit breaker:
- **Closed**: Normal operation
- **Open**: Fast-fail after 50% error rate
- **Half-Open**: Test recovery after timeout

### 4. Degraded Enrichment
System continues processing with partial metadata when sources fail, ensuring zero log loss.

## Configuration

### Circuit Breaker Settings
Edit `enrichment-service/src/main/resources/application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      redis-hostname:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

### Kafka Streams Tuning
```yaml
spring:
  kafka:
    streams:
      properties:
        commit.interval.ms: 1000
        cache.max.bytes.buffering: 10485760
```

## Production Readiness Checklist

- [x] Circuit breakers for all external dependencies
- [x] Comprehensive metrics and observability
- [x] Graceful degradation on failures
- [x] Zero log loss guarantee
- [x] Horizontal scalability (Kafka consumer groups)
- [x] Schema versioning for compatibility
- [x] Integration test suite
- [x] Load testing framework
- [x] Health checks and readiness probes

## Scaling Strategy

### Horizontal Scaling
```bash
# Run multiple enrichment service instances
java -jar enrichment-service/target/enrichment-service-1.0.0.jar --server.port=8081
java -jar enrichment-service/target/enrichment-service-1.0.0.jar --server.port=8091
java -jar enrichment-service/target/enrichment-service-1.0.0.jar --server.port=8101
```

Kafka Streams automatically rebalances partitions across instances.

### Capacity Planning
- **Single instance**: 10K logs/sec
- **Target load**: 100K logs/sec
- **Required instances**: 10+ with headroom

## Troubleshooting

### Logs not being enriched
1. Check Kafka topic exists: `docker exec -it $(docker ps -qf "name=kafka") kafka-topics --list --bootstrap-server localhost:9092`
2. Verify enrichment service is consuming: Check logs for "Enriching log: ..."
3. Check circuit breaker state: `curl http://localhost:8081/actuator/health`

### High latency
1. Check Redis connection: `docker exec -it $(docker ps -qf "name=redis") redis-cli PING`
2. Review cache hit rates in metrics
3. Check PostgreSQL connection pool utilization

### Missing metadata
1. Verify metadata exists in database: `docker exec -it $(docker ps -qf "name=postgres") psql -U postgres -d enrichment_db -c "SELECT * FROM host_metadata;"`
2. Check Redis cache: `docker exec -it $(docker ps -qf "name=redis") redis-cli KEYS "*"`

## Next Steps: Day 22

Tomorrow we'll build a **distributed log storage cluster** with:
- Multi-node file replication
- Partition strategies for scalable storage
- Read-your-writes consistency
- Node failure recovery

The enriched logs from today become the input to tomorrow's distributed storage layer.

## License
MIT License - Educational purposes for 254-Day System Design Course
