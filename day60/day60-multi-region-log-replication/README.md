# Day 60: Multi-Region Log Replication

A production-grade distributed log processing system demonstrating active-active multi-region replication, watermark-based event reordering, and bloom-filter deduplication.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway (:8080)                      │
│         Region-aware routing + health checks + retries          │
└────────────────┬──────────────────────────┬────────────────────┘
                 │                          │
        X-Region: region-a         X-Region: region-b
                 │                          │
                 ▼                          ▼
      ┌──────────────────┐      ┌──────────────────┐
      │  Log Producer A  │      │  Log Producer B  │
      │    (:8081)       │      │    (:8083)       │
      └────────┬─────────┘      └────────┬─────────┘
               │                         │
               ▼                         ▼
      ┌──────────────────┐      ┌──────────────────┐
      │  Kafka Region A  │◄────►│  Kafka Region B  │
      │   (:9092)        │ MM2  │   (:9093)        │
      └────────┬─────────┘      └────────┬─────────┘
               │                         │
               ▼                         ▼
      ┌──────────────────┐      ┌──────────────────┐
      │  Log Consumer A  │      │  Log Consumer B  │
      │    (:8082)       │      │    (:8084)       │
      └────────┬─────────┘      └────────┬─────────┘
               │                         │
               └───────────┬─────────────┘
                           ▼
              ┌─────────────────────┐
              │    PostgreSQL       │
              │     (:5432)         │
              └─────────────────────┘
```

## Quick Start

```bash
# 1. Generate all project files
chmod +x setup.sh && ./setup.sh

# 2. Smoke test
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -H 'X-Region: region-a' \
  -d '{"serviceName":"my-svc","level":"INFO","message":"Hello","correlationId":"trace-1"}'

# 3. Run load tests (simulates split-brain + recovery)
./load-test.sh

# 4. Run integration tests
./integration-tests/run-integration-tests.sh

# 5. View metrics
# Grafana: http://localhost:3000  (dashboard auto-provisioned)
# Prometheus: http://localhost:9090
```

## Key Design Decisions

### Active-Active Replication
Both regions accept writes simultaneously. MirrorMaker 2 replicates in both directions. This maximizes availability (RPO ≈ replication lag) at the cost of requiring deduplication after split-brain recovery.

### Bloom Filter Deduplication
A Guava `BloomFilter` provides O(1) probabilistic duplicate detection locally. Redis `SETNX` provides exact cross-instance deduplication with a 24-hour TTL. Together they handle millions of events/day with bounded memory.

### Watermark-Based Reordering
A 1-second configurable window holds events and flushes them in timestamp order. Events arriving after the watermark advances are classified as late arrivals and persisted directly (with a flag) for reconciliation.

### Partition Key = correlationId
All events sharing a distributed trace land in the same Kafka partition, preserving intra-trace ordering even across rebalances.

## Monitoring

| Metric | What It Tells You |
|--------|-------------------|
| `log_producer_send_total` | Total events produced per region |
| `dedup_hits_total` | How many duplicates the system caught |
| `reorder_late_arrivals_total` | Events that arrived after the watermark |
| `reorder_buffer_size` | Current buffer depth (watch for growth) |
| `gateway_routing_fallbacks_total` | How often the gateway had to switch regions |
| `log_producer_send_duration_seconds` | Producer latency (p99 is your SLO target) |

## Stopping the Stack

```bash
docker compose down -v  # Removes containers and volumes
```

## Tuning Parameters

| Parameter | Default | Effect |
|-----------|---------|--------|
| `app.reorder.window-ms` | 1000 | Reorder buffer window. ↓ = lower latency, ↓ ordering. |
| `app.dedup.bloom.expected-insertions` | 1,000,000 | Bloom filter capacity before FP rate degrades. |
| `app.dedup.bloom.false-positive-rate` | 0.0001 | 1 in 10,000 chance of false positive. |
| `app.dedup.redis.ttl-hours` | 24 | How long dedup keys live in Redis. |
