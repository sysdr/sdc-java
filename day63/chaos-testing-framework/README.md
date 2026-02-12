# Chaos Testing Framework for Distributed Log Processing

## Architecture Overview

This system implements a production-grade chaos engineering framework for testing resilience in a distributed log processing system.

### Components

1. **API Gateway** (Port 8080)
   - Entry point for log ingestion
   - Routes requests to producer service

2. **Log Producer** (Port 8081)
   - Publishes log events to Kafka
   - Circuit breaker protection for Kafka failures
   - Metrics: success/failure counters, latency

3. **Log Consumer** (Port 8082)
   - Consumes from Kafka topic
   - Persists to PostgreSQL
   - Caches in Redis
   - Circuit breakers for both PostgreSQL and Redis

4. **Chaos Engine**
   - Orchestrates chaos experiments
   - Failure injection: service kills, network latency, resource exhaustion
   - Validates system resilience and recovery

5. **Infrastructure**
   - Kafka + Zookeeper
   - PostgreSQL
   - Redis
   - Prometheus (metrics)
   - Grafana (dashboards)

## Quick Start

### 1. Start Infrastructure

```bash
./setup.sh
```

This starts all Docker containers (Kafka, PostgreSQL, Redis, Prometheus, Grafana).

### 2. Build All Services

```bash
mvn clean install
```

### 3. Start Services (in separate terminals)

```bash
# Terminal 1: Producer
cd log-producer && mvn spring-boot:run

# Terminal 2: Consumer
cd log-consumer && mvn spring-boot:run

# Terminal 3: Gateway
cd api-gateway && mvn spring-boot:run
```

### 4. Verify System

```bash
./integration-tests/test-end-to-end.sh
```

### 5. Run Chaos Tests

```bash
cd chaos-engine
mvn test
```

## Chaos Experiments

### Experiment 1: Producer Service Kill

**Hypothesis**: System recovers within 30 seconds after producer restart

```java
ChaosExperiment experiment = new ChaosExperiment();
experiment.setName("Producer Kill Test");
experiment.setTargets(List.of("log-producer"));
experiment.setFailureType(FailureType.SERVICE_KILL);
experiment.setDuration(Duration.ofSeconds(10));
```

**Expected Behavior**:
- Circuit breaker opens immediately
- Requests fail fast (< 10ms)
- Service restarts automatically
- Circuit transitions to half-open
- Full recovery within 30s

### Experiment 2: Kafka Network Latency

**Hypothesis**: P95 latency stays under 2000ms during 250ms network delay

```java
experiment.setFailureType(FailureType.NETWORK_LATENCY);
experiment.setTargets(List.of("kafka"));
```

**Expected Behavior**:
- Producer detects slow responses
- Buffer fills, backpressure activates
- Some requests timeout and trigger circuit breaker
- System degrades gracefully, no crashes

### Experiment 3: PostgreSQL Failure

**Hypothesis**: Consumer continues operating with cache-only mode

**Expected Behavior**:
- PostgreSQL circuit breaker opens
- Fallback writes to Redis (DLQ)
- Zero data loss
- System recovers when PostgreSQL returns

## Load Testing

```bash
./load-test.sh
```

Sends 100 concurrent requests to verify:
- Throughput under load
- Circuit breaker behavior
- Latency distribution

## Monitoring

### Prometheus
- URL: http://localhost:9090
- Metrics: `log_events_sent_success`, `log_events_processed`, `resilience4j_circuitbreaker_state`

### Grafana
- URL: http://localhost:3000
- Credentials: admin/admin
- Import dashboards for circuit breaker states, latency percentiles, error rates

## Key Resilience Patterns Demonstrated

1. **Circuit Breakers**: Prevent cascade failures (Kafka, PostgreSQL, Redis)
2. **Backpressure**: Producer buffer management
3. **Graceful Degradation**: Cache-only mode when DB fails
4. **Fail-Fast**: Circuit open requests return immediately
5. **Health Checks**: Docker health checks for auto-restart
6. **Observability**: Comprehensive metrics for chaos validation

## System Design Insights

### CAP Theorem Trade-offs
- **Consistency**: Kafka idempotent producer (at-most-once)
- **Availability**: Circuit breakers fail-open for read path
- **Partition Tolerance**: Service isolation via circuit breakers

### Failure Modes Tested
1. Slow failures (network latency) vs fast failures (service crash)
2. Transient errors (network blip) vs permanent failures (DB down)
3. Resource exhaustion (OOM, CPU saturation)
4. Cascading failures (one service crash affects others)

### Recovery Patterns
- **Auto-restart**: Docker health checks
- **Circuit breaker state machine**: Closed → Open → Half-Open
- **Exponential backoff**: Resilience4j retry configuration
- **Dead Letter Queues**: Cache-based DLQ for PostgreSQL failures

## Troubleshooting

**Services won't start**: Check Docker resources (4GB+ RAM recommended)

**Kafka connection refused**: Wait 30s for Kafka to initialize after `docker compose up`

**Circuit breaker stays open**: Check service logs, may need to restart dependent service

**Tests fail**: Verify all services are healthy with `./integration-tests/test-end-to-end.sh`

## Production Deployment Considerations

1. **Blast Radius Control**: Start chaos in 1% of production, expand gradually
2. **Monitoring**: Alert on circuit breaker state changes
3. **Runbooks**: Document recovery procedures for each experiment
4. **Scheduled Chaos**: Run during business hours with SRE oversight
5. **Rollback Plan**: One-click abort for all active experiments

## License

MIT
