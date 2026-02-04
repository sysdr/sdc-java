# Day 62: Backpressure Mechanisms for Load Management

## System Architecture

This distributed log processing system implements production-grade backpressure mechanisms:

- **Adaptive Rate Limiting**: Token bucket algorithm adjusts based on consumer lag
- **Consumer Lag Monitoring**: Real-time lag tracking with automatic throttling
- **Circuit Breaker Integration**: Backpressure triggered when downstream services fail
- **Reactive Backpressure**: Non-blocking flow control using Spring WebFlux

## Quick Start

### 1. Start Infrastructure

```bash
./setup.sh
```

This starts Kafka, PostgreSQL, Redis, Prometheus, and Grafana.

### 2. Build and Run Services

Terminal 1 - API Gateway:
```bash
cd api-gateway
mvn clean install
mvn spring-boot:run
```

Terminal 2 - Log Consumer:
```bash
cd log-consumer
mvn clean install
mvn spring-boot:run
```

### 3. Verify System

```bash
# Check health
curl http://localhost:8080/api/logs/health | jq .

# Send test log
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{"correlationId":"test-1","severity":"INFO","message":"Test","source":"cli","timestamp":1234567890000}'
```

## Testing Backpressure

### Integration Tests

```bash
./integration-tests/test-backpressure.sh
```

### Load Testing

```bash
./load-test.sh
```

Monitor the system during load:
- Consumer lag increases
- Rate limiter adjusts dynamically
- 429 responses appear when critically overloaded

## Monitoring

- **Prometheus**: http://localhost:9090
  - Query: `kafka_consumer_lag`
  - Query: `rate_limiter_available_permissions`
  
- **Grafana**: http://localhost:3000 (admin/admin)
  - Dashboard: "Backpressure Monitoring"

## Key Metrics

- `kafka.consumer.lag`: Current consumer lag
- `rate_limiter.available_permissions`: Token bucket state
- `resilience4j.circuitbreaker.state`: Circuit breaker status
- HTTP 429 responses: Rate limit rejections

## Backpressure Behavior

1. **Normal Operation** (lag < 10K):
   - Rate limit: 1000 req/s
   - All requests accepted
   
2. **Degraded** (lag 10K-50K):
   - Rate limit: Proportionally reduced
   - Some 429 responses
   
3. **Critical** (lag > 50K):
   - Rate limit: 100 req/s minimum
   - Circuit breaker may open
   - Consumer pauses if DB fails

## Architecture Highlights

- **Token Bucket Rate Limiting**: Fast, in-memory, adjusts every 5s
- **Reactive Processing**: WebFlux for non-blocking backpressure
- **Bounded Queues**: CallerRuns policy prevents memory exhaustion
- **HikariCP**: 20 connection pool with 5s timeout

## Troubleshooting

**High lag not decreasing**:
- Check consumer logs for errors
- Verify database connection pool not exhausted
- Check circuit breaker state

**All requests getting 429**:
- Consumer likely far behind
- Check `curl http://localhost:8080/api/logs/health`
- Reduce load or scale consumers

**Circuit breaker stuck open**:
- Database may be down
- Check PostgreSQL: `docker logs $(docker ps -q -f name=postgres)`

## Cleanup

```bash
docker-compose down -v
```
