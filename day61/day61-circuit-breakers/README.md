# Day 61 — Circuit Breakers for Handling Component Failures

## Architecture Overview

```
┌─────────────┐   HTTP    ┌──────────────┐  Kafka   ┌──────────────┐
│ API Gateway │──────────▶│  Log Producer │─────────▶│ Log Consumer │
│   :8080     │           │    :8081     │          │    :8082     │
└─────────────┘           └──────────────┘          └──────┬───────┘
                                                           │
                                              ┌────────────┼────────────┐
                                              ▼            ▼            ▼
                                         PostgreSQL     Redis       (metrics)
                                           :5432        :6379     Prometheus
                                                                    :9090
```

### Circuit Breakers by Service

| Service       | Target     | Window Type | Threshold | Wait Duration |
|---------------|------------|-------------|-----------|---------------|
| api-gateway   | log-producer (HTTP) | TIME_BASED (10s) | 50% | 15s |
| log-producer  | Kafka broker        | TIME_BASED (5s)  | 50% | 15s |
| log-consumer  | PostgreSQL          | TIME_BASED (10s) | 60% | 30s |
| log-consumer  | Redis               | COUNT_BASED (5)  | 60% | 10s |

### Fallback Strategies

- **Gateway → Producer**: Return 202 (accepted), log to local dead-letter buffer.
- **Producer → Kafka**: Buffer in-memory (max 1000 events), reconcile on 30s timer.
- **Consumer → PostgreSQL**: Buffer in-memory (max 500 events), reconcile on 20s timer.
- **Consumer → Redis**: Skip cache entirely, serve from DB on next read.

## Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin)
- `curl` (for load tests and integration tests)
- Java 17+ and Maven 3.8+ (if building locally without Docker)

## Quick Start

```bash
# 1. Generate the project (if not already done)
chmod +x setup.sh && ./setup.sh

# 2. Bootstrap infrastructure + application
cd day61-circuit-breakers
chmod +x setup.sh && ./setup.sh

# 3. Wait 60-90 seconds for all services to start

# 4. Run integration tests
./integration-tests/run-tests.sh

# 5. Run load tests
./load-test.sh            # Normal: 100 requests
./load-test.sh --burst    # Burst: 500 requests
./load-test.sh --levels   # All log levels
```

## Observability

| Dashboard | URL |
|-----------|-----|
| Grafana   | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Gateway Metrics | http://localhost:8080/actuator/prometheus |
| Producer Metrics | http://localhost:8081/actuator/prometheus |
| Consumer Metrics | http://localhost:8082/actuator/prometheus |

## Simulating Failures

### Kill PostgreSQL
```bash
docker stop postgres
# Watch: consumer breaker trips, events buffer in memory
# Watch Grafana: consumer.postgres.write.fallback.total rises

docker start postgres
# Wait up to 30s (wait duration) + 20s (reconciler)
# Buffered events flush to PostgreSQL
```

### Kill Redis
```bash
docker stop redis
# Redis breaker trips. Reads skip cache, writes are no-ops.
# Pipeline continues at full throughput — only cache latency increases.

docker start redis
```

### Kill Kafka
```bash
docker stop kafka
# Producer breaker trips. Events buffer locally (max 1000).
# Gateway returns 202 — callers see no error.
# After Kafka restarts + 30s reconciler, buffered events are produced.

docker start kafka
```

## Key Architectural Decisions

1. **Independent breakers**: PostgreSQL and Redis breakers are independent.
   Redis being down should never affect PostgreSQL writes, and vice versa.

2. **Retry inside breaker**: Retries are innermost in the composition chain.
   Exhausted retries count as ONE failure to the circuit breaker.

3. **Availability over consistency**: All fallbacks accept data and buffer locally.
   We never block the pipeline. Reconciliation happens asynchronously.

4. **Time-based vs count-based**: High-latency services (Kafka, PostgreSQL) use
   time-based windows. Binary-failure services (Redis) use count-based.

## Teardown

```bash
docker compose down -v   # Stop all containers and remove volumes
```
