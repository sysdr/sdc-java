# Day 23: Partitioned Log Storage System

A production-ready distributed log processing system implementing composite partitioning (time + source) for optimal query performance.

## Architecture

**Composite Partitioning Strategy:**
- Daily time-based parent partitions
- 256 source hash-based sub-partitions per day
- Automatic partition creation 24 hours in advance
- Partition-aware query routing for minimal I/O

**Components:**
- **Log Producer**: Ingests logs via REST API, routes to Kafka with partition keys
- **Log Consumer**: Batch-writes logs to partitioned PostgreSQL tables
- **Partition Manager**: Automated partition lifecycle management
- **Query Service**: Partition-pruned queries for 100-10,000x speedup

## Performance Targets

- **Write Throughput**: 50,000 inserts/second
- **Query Latency (time-range)**: <100ms for 24-hour window
- **Query Latency (source-specific)**: <10ms single source
- **Partition Pruning**: 256x reduction in data scanned

## Quick Start

### 1. Setup Infrastructure
```bash
# If not already generated, run the generator script from day23 directory:
# ./setup.sh
# Then:
cd partitioned-log-storage
./setup.sh
```

### 2. Start Services (in separate terminals)
```bash
# Terminal 1: Partition Manager (must start first)
java -jar partition-manager/target/partition-manager-1.0.0.jar

# Terminal 2: Consumer
java -jar log-consumer/target/log-consumer-1.0.0.jar

# Terminal 3: Producer
java -jar log-producer/target/log-producer-1.0.0.jar

# Terminal 4: Query Service
java -jar query-service/target/query-service-1.0.0.jar
```

### 3. Start Dashboard (Optional)
```bash
cd dashboard-server
npm install
npm start
```
Access the dashboard at: http://localhost:3001

### 4. Run Load Test
```bash
./load-test.sh
```

## Key Architectural Patterns

### 1. Composite Partitioning
```
logs (partitioned by log_date)
  ├── logs_2024_11_28 (partitioned by source_hash)
  │   ├── logs_2024_11_28_p000 (hash 0)
  │   ├── logs_2024_11_28_p001 (hash 1)
  │   └── ... (254 more)
  └── logs_2024_11_29 (partitioned by source_hash)
      └── ... (256 partitions)
```

**Trade-off:** Query "payment-service errors last 3 days" scans 3 partitions instead of 768 (256x reduction).

### 2. Pre-Provisioning
Partition Manager creates partitions 24 hours in advance. Prevents write failures during traffic spikes.

**Why:** At 50K writes/sec, a missing partition causes cascading failure—queued writes overflow in seconds.

### 3. Batch Writing
Consumer batches 1000 writes to amortize partition routing cost from 2ms to 0.002ms per write.

**Math:** 10K writes/sec × 2ms = 20 seconds of CPU per second (impossible). Batching makes it 0.02 seconds.

### 4. Partition-Aware Queries
Query Service analyzes predicates and builds UNION queries across minimal partition set.

**Example:**
- Without pruning: Scan all 768 partitions (3 days × 256 sources)
- With time pruning: Scan 256 partitions (3 days × all sources)
- With time + source pruning: Scan 3 partitions (3 days × 1 source)

## Testing Partition Performance

### Verify Partitions Exist
```bash
docker compose exec postgres psql -U loguser -d logdb -c "
  SELECT tablename FROM pg_tables 
  WHERE schemaname = 'public' AND tablename LIKE 'logs_%'
  ORDER BY tablename;
"
```

### Check Partition Sizes
```bash
docker compose exec postgres psql -U loguser -d logdb -c "
  SELECT 
    tablename,
    pg_size_pretty(pg_total_relation_size('public.' || tablename)) AS size
  FROM pg_tables
  WHERE schemaname = 'public' AND tablename LIKE 'logs_2024%'
  ORDER BY pg_total_relation_size('public.' || tablename) DESC
  LIMIT 20;
"
```

### Test Query Performance
```bash
# Source-specific query (should hit 1 partition per day)
curl -X POST http://localhost:8084/api/query/logs \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2024-11-28T00:00:00",
    "endTime": "2024-11-29T00:00:00",
    "source": "payment-service",
    "level": "ERROR",
    "limit": 100
  }' | jq .

# Time-range query (scans all source partitions)
curl -X POST http://localhost:8084/api/query/logs \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2024-11-28T12:00:00",
    "endTime": "2024-11-28T13:00:00",
    "limit": 100
  }' | jq .
```

### Explain Query Plan
```bash
docker compose exec postgres psql -U loguser -d logdb -c "
  EXPLAIN SELECT * FROM logs 
  WHERE log_date >= '2024-11-28' 
    AND log_date < '2024-11-29'
    AND source = 'payment-service'
  LIMIT 10;
"
```

Look for "Partitions scanned: N" in output. Should be 1 with source filter, 256 without.

## Production Monitoring

### Web Dashboard
Access the professional web dashboard at: http://localhost:3001

Features:
- Real-time metrics graphs (logs produced, written, query duration)
- Partition statistics and distribution
- Interactive log query interface
- System health monitoring
- Operations overview

See [dashboard-server/README.md](dashboard-server/README.md) for details.

### Grafana Dashboards
Access: http://localhost:3000 (admin/admin)

Key metrics:
- **Partition Creation Lag**: Should be <1 hour
- **Partition Size Distribution**: Largest partition should be <2x median
- **Query Partition Hit Rate**: % queries scanning <10% of partitions
- **Write Distribution**: No partition receives >5% of writes

### Critical Alerts

**Partition Creation Failure:**
```promql
increase(partitions_created_total[1h]) == 0
```

**Hot Partition Detected:**
```promql
max(partition_write_rate) > 2 * avg(partition_write_rate)
```

**Partition Pruning Ineffective:**
```promql
histogram_quantile(0.99, query_duration_bucket) > 1
```

## Common Issues

### Issue: Queries Scan All Partitions
**Symptom:** Slow queries despite filters
**Diagnosis:** 
```sql
EXPLAIN SELECT * FROM logs WHERE source = 'payment-service';
-- Shows all partitions scanned
```
**Fix:** Verify partition constraints exist:
```sql
SELECT conname, conrelid::regclass, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conname LIKE '%partition_check%';
```

### Issue: Partition Creation Falls Behind
**Symptom:** Writes fail with "relation does not exist"
**Diagnosis:** Check partition manager logs
**Fix:** Manual partition creation:
```bash
curl -X POST http://localhost:8083/actuator/health
# Check last successful run timestamp
```

### Issue: Hot Partition from Traffic Spike
**Symptom:** Single partition much larger than others
**Diagnosis:** Check partition sizes (see above)
**Fix:** Implement dynamic partition splitting (Day 24 topic)

## Scaling to Production

**Netflix Scale (500B events/day):**
- 100K source services → Need 100K sub-partitions
- Daily partitions → 365 parent partitions/year
- Pre-create partitions 72 hours in advance
- Use partition compression for >30 day data

**Trade-offs at Scale:**
- More partitions = better pruning, higher overhead
- Sweet spot: `partition_count = expected_nodes × 8`
- Monitor: If query planning takes >10ms, too many partitions

## Integration Tests

Run comprehensive tests:
```bash
./integration-tests/test-partition-performance.sh
```

Tests verify:
1. Partitions created successfully
2. Data distributed across partitions
3. Partition pruning working
4. Query performance targets met

## Next Steps: Day 24

Tomorrow we'll implement consistent hashing to solve partition imbalance. When new sources appear or traffic patterns shift, our hash-based partitioning can create hot spots. Consistent hashing with virtual nodes ensures even distribution and enables seamless partition rebalancing.

## System Requirements

- Java 17+
- Docker & Docker Compose
- 8GB RAM (for load testing)
- 20GB disk space (for partition storage)

## References

- PostgreSQL Partitioning: https://www.postgresql.org/docs/current/ddl-partitioning.html
- Partition Pruning: https://www.postgresql.org/docs/current/ddl-partitioning.html#DDL-PARTITION-PRUNING
- Index Management: https://www.postgresql.org/docs/current/sql-createindex.html
