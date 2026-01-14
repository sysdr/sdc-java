# Log Compaction State Management System

## Overview

Production-grade distributed state management system using Kafka log compaction for maintaining current entity state across microservices. Demonstrates how to use compacted topics as self-maintaining state stores with bounded storage.

## Architecture

### System Components

1. **State Producer Service** (Port 8081)
   - REST API for entity state updates and deletions
   - Produces keyed messages to compacted Kafka topic
   - Tombstone support for entity deletion
   - Idempotent producer with exactly-once semantics

2. **State Consumer Service** (Port 8082)
   - Consumes from compacted topic
   - Materializes state to PostgreSQL
   - Maintains Redis cache for fast queries
   - Handles both updates and tombstones

3. **State Query API** (Port 8083)
   - REST API for querying current entity state
   - Cache-aside pattern with Redis (sub-5ms reads)
   - Circuit breaker for resilience
   - Database fallback on cache miss

4. **Infrastructure**
   - Kafka with log compaction enabled
   - PostgreSQL for materialized state
   - Redis for query caching
   - Prometheus + Grafana for monitoring

### Log Compaction Configuration

```yaml
cleanup.policy: compact
min.cleanable.dirty.ratio: 0.5      # Compact at 50% duplicates
segment.ms: 86400000                # 24-hour segments
delete.retention.ms: 86400000       # 24-hour tombstone retention
min.compaction.lag.ms: 0            # Immediate compaction eligibility
```

## Quick Start

### Prerequisites
- Docker and Docker Compose
- 8GB RAM available
- Ports 8081-8083, 9090, 3000, 5432, 6379, 9092 available

### 1. Generate and Start System
```bash
chmod +x setup.sh
./setup.sh
```

System boots in ~2 minutes. Services available at:
- State Producer: http://localhost:8081
- State Query API: http://localhost:8083
- Grafana: http://localhost:3000 (admin/admin)

### 2. Verify Health
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health
```

### 3. Run Integration Tests
```bash
./integration-tests/test-state-lifecycle.sh
```

### 4. Run Load Test
```bash
./load-test.sh
```

Generates 10,000 state updates across 1,000 unique entities, demonstrating compaction keeping only latest state.

## Usage Examples

### Update Entity State
```bash
curl -X POST http://localhost:8081/api/state/update \
  -H "Content-Type: application/json" \
  -d '{
    "entityId": "user-123",
    "entityType": "user",
    "status": "active",
    "attributes": {"plan": "premium"},
    "timestamp": "2025-01-07T10:00:00Z",
    "version": 1
  }'
```

### Query Entity State
```bash
curl http://localhost:8083/api/query/entity/user-123
```

### Delete Entity (Tombstone)
```bash
curl -X DELETE http://localhost:8081/api/state/user/user-123
```

### Query Statistics
```bash
curl http://localhost:8083/api/query/stats
```

## Monitoring

### Grafana Dashboards
Visit http://localhost:3000 (admin/admin)

Key metrics:
- State updates per second
- Tombstones processed
- Cache hit rate (target: >90%)
- Query latency P99 (target: <50ms)
- Compaction lag

### Prometheus Queries
```promql
# State update rate
rate(state_updates_total[1m])

# Cache effectiveness
rate(state_query_cache_hits_total[1m]) / 
  (rate(state_query_cache_hits_total[1m]) + rate(state_query_cache_misses_total[1m]))

# Query latency
histogram_quantile(0.99, rate(state_query_time_seconds_bucket[1m]))
```

## Performance Characteristics

### Expected Throughput
- **State Updates:** 40,000-50,000/sec
- **State Queries:** 100,000/sec (cached)
- **Query Latency:** <5ms (cache hit), 20-50ms (cache miss)

### Storage Patterns
- **Compaction Effectiveness:** Maintains 1 state per entity regardless of updates
- **Before Compaction:** 10,000 updates × 1KB = 10MB per entity
- **After Compaction:** 1KB per entity (99.99% reduction)

## How Compaction Works

1. **Producer sends keyed update:** `{key: "user:123", value: {...}}`
2. **Kafka appends to active segment**
3. **Background cleaner compacts older segments:** Keeps only latest value per key
4. **Tombstone deletion:** `{key: "user:123", value: null}` marks deletion
5. **Tombstone removal:** After `delete.retention.ms`, tombstone is removed
6. **Consumer rehydration:** Reading from offset 0 gets only current state

## State Rehydration

New consumer instances can rebuild complete state by consuming from offset 0:

```bash
# Stop consumer
docker-compose stop state-consumer

# Clear materialized state
docker-compose exec postgres psql -U stateuser -d statedb -c "TRUNCATE entity_states;"

# Restart consumer (rehydrates from compacted log)
docker-compose start state-consumer
```

Rehydration time: ~100 seconds for 10M entities @ 1KB each

## Troubleshooting

### Compaction Not Running
```bash
# Check cleaner metrics
docker-compose exec kafka kafka-run-class kafka.tools.JmxTool \
  --jmx-url service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi \
  --object-name kafka.log:type=LogCleaner,name=*
```

### High Compaction Lag
- Increase `log.cleaner.threads` (default: 1)
- Adjust `min.cleanable.dirty.ratio` (lower = more frequent)
- Check disk I/O saturation

### State Query Latency Issues
- Verify Redis cache hit rate (target: >90%)
- Check PostgreSQL connection pool
- Monitor circuit breaker state

## Production Considerations

### Capacity Planning
- **Entities:** 10M entities
- **State Size:** 1KB average
- **Storage:** 15GB (10GB compacted + 50% working set)
- **Partitions:** 24 (400K entities per partition)
- **Consumers:** 3-6 instances (parallel processing)

### Disaster Recovery
1. **Kafka topic backup:** Compacted log is source of truth
2. **PostgreSQL snapshots:** For fast bootstrap
3. **Redis cache:** Ephemeral, rebuilt automatically

### Scaling Strategies
- **Horizontal:** Add consumer instances (up to partition count)
- **Vertical:** Increase Kafka broker disk I/O for compaction
- **Partition:** Increase topic partitions for higher parallelism

## Clean Up
```bash
docker-compose down -v
```

## Key Learnings

1. **Log compaction = self-maintaining state store:** No need for manual cleanup
2. **Tombstones critical for deletion:** Must set `delete.retention.ms` > max consumer lag
3. **Cache-aside pattern essential:** 95%+ cache hit rate achievable
4. **Rehydration enables recovery:** Complete state rebuilt from compacted log
5. **Partitioning determines parallelism:** Consumer instances ≤ partition count

## License

MIT License - See LICENSE file
