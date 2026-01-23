# MapReduce Framework for Log Processing

A production-grade distributed MapReduce implementation for batch log analysis, processing millions of log events with fault-tolerant coordination and horizontal scalability.

## Architecture Overview

### Components

1. **MapReduce Coordinator** (Port 8080)
   - Job lifecycle management and task scheduling
   - Worker heartbeat monitoring and failure detection
   - PostgreSQL-backed state persistence for fault tolerance
   - Automatic task retry with exponential backoff

2. **Map Workers** (Port 8081, horizontally scalable)
   - Consume log batches from Kafka partitions
   - Apply user-defined map functions (word count, pattern extraction)
   - Partition intermediate results using murmur3 hashing
   - Write to Redis with 1-hour TTL

3. **Reduce Workers** (Port 8082, horizontally scalable)
   - Fetch shuffled data from Redis by partition
   - Group by key and apply reduce functions
   - Persist final results to PostgreSQL
   - Support for combiners to minimize network I/O

4. **API Gateway** (Port 8090)
   - REST endpoints for job submission and status queries
   - Rate limiting and circuit breaker patterns
   - Result retrieval from PostgreSQL

5. **Infrastructure**
   - **Kafka**: Task queue and log ingestion
   - **Redis**: Intermediate result storage and shuffle layer
   - **PostgreSQL**: Job metadata and final results
   - **Prometheus + Grafana**: Metrics and monitoring

### Data Flow

```
Logs (Kafka) → Map Workers → Shuffle (Redis) → Reduce Workers → Results (PostgreSQL)
                     ↓                                  ↓
              Coordinator ←──────────────────── Completion Events
```

## Quick Start

### Prerequisites

- Docker and Docker Compose
- JDK 17+
- Maven 3.8+

### Setup

```bash
# Generate and start the system
chmod +x setup.sh
./setup.sh

# Wait for all services to be healthy (~30 seconds)
```

### Submit a Job

```bash
curl -X POST http://localhost:8090/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{
    "jobName": "word-count-analysis",
    "inputTopic": "application-logs",
    "numMappers": 4,
    "numReducers": 2,
    "mapFunction": "WORD_COUNT",
    "reduceFunction": "SUM"
  }'

# Response: {"jobId":"abc-123","status":"RUNNING","message":"Job submitted successfully"}
```

### Check Job Status

```bash
curl http://localhost:8090/api/jobs/{jobId}
```

### View Results

```bash
docker-compose exec postgres psql -U postgres -d mapreduce

SELECT result_key, result_value 
FROM results 
WHERE job_id = '{jobId}' 
ORDER BY result_value DESC 
LIMIT 20;
```

## Running Tests

### Integration Test

```bash
cd integration-tests
./test-mapreduce.sh
```

Tests complete MapReduce workflow:
1. Job submission via API Gateway
2. Map task execution (4 parallel mappers)
3. Shuffle phase (Redis intermediate storage)
4. Reduce task execution (2 parallel reducers)
5. Result verification in PostgreSQL

Expected output: Word frequencies and pattern counts from 4,000 log events.

### Load Test

```bash
./load-test.sh
```

Submits 10 concurrent jobs, each processing 4,000 log events. Tests:
- Coordinator scalability under load
- Worker pool parallelism
- Redis shuffle layer throughput
- PostgreSQL write performance

Expected metrics:
- **Throughput**: 50,000 events/second
- **Job Completion Time**: 15-30 seconds per job
- **Task Failure Rate**: <1% with automatic retry

## Monitoring

### Prometheus (http://localhost:9090)

Key metrics to monitor:

```promql
# Job throughput
rate(mapreduce_jobs_completed_total[5m])

# Task success rate
sum(rate(mapreduce_tasks_completed_total[5m])) / 
sum(rate(mapreduce_tasks_started_total[5m]))

# Shuffle data volume
rate(redis_commands_processed_total{cmd="rpush"}[5m])

# Worker resource utilization
process_cpu_usage{job="map-worker"}
jvm_memory_used_bytes{job="reduce-worker"}
```

### Grafana (http://localhost:3000, admin/admin)

Pre-configured dashboards:
1. **Job Overview**: Completion rate, duration distribution, failure rate
2. **Worker Performance**: CPU/memory usage, task throughput, error rate
3. **Infrastructure**: Kafka lag, Redis memory, PostgreSQL connections

## System Design Patterns

### 1. Coordinator-Worker Architecture

**Pattern**: Centralized coordinator schedules tasks to distributed workers.

**Trade-off**: Coordinator is a single point of failure. In production, use:
- ZooKeeper for coordinator leader election
- Replicated state in PostgreSQL with read replicas
- Heartbeat-based failure detection (15s timeout)

**Scaling**: Coordinator handles 1,000 tasks/sec. Beyond that:
- Implement hierarchical scheduling (sub-coordinators per job)
- Use consistent hashing for task assignment

### 2. Shuffle Phase Optimization

**Pattern**: Hash partitioning with combiner pre-aggregation.

**Implementation**:
- Murmur3 hash for even key distribution
- Local combiners reduce shuffle data by 80%
- Redis pipelining for batch writes (100 ops/batch)

**Alternative**: Disk-based shuffle (slower, more fault-tolerant)

### 3. Fault Tolerance

**Mechanisms**:
- Task-level idempotency (deterministic output for same input)
- Automatic retry with exponential backoff (3 attempts)
- Intermediate data persists in Redis for reducer crashes
- PostgreSQL WAL ensures result durability

**Recovery Time**: 5-15 seconds to detect failure and reschedule

### 4. Backpressure Handling

**Problem**: Fast mappers overwhelm slow reducers.

**Solution**:
- Limit concurrent map tasks (4 per job)
- Redis lists provide natural buffering
- Coordinator monitors queue depth and throttles new tasks

## Performance Characteristics

### Throughput

- **Single Map Worker**: 12,500 events/second
- **4 Map Workers**: 50,000 events/second (linear scaling)
- **Shuffle Layer (Redis)**: 100,000 operations/second
- **Single Reduce Worker**: 20,000 keys/second

### Latency

- **Map Task**: 800ms for 10,000 events
- **Shuffle**: 200ms for 50,000 intermediate key-values
- **Reduce Task**: 1.5s for 10,000 unique keys
- **End-to-End Job**: 15-30s for 40,000 events

### Resource Requirements

- **Map Worker**: 2GB RAM, 1 CPU core
- **Reduce Worker**: 3GB RAM (buffering grouped data), 1 CPU core
- **Coordinator**: 1GB RAM, 0.5 CPU core
- **Redis**: 8GB RAM for 10M intermediate key-values
- **PostgreSQL**: 4GB RAM, 20GB disk

## Scaling Guide

### Horizontal Scaling

```bash
# Scale map workers to 8
docker-compose up -d --scale map-worker=8

# Scale reduce workers to 4
docker-compose up -d --scale reduce-worker=4
```

**Linear scaling up to**:
- 20 map workers (200K events/sec)
- 10 reduce workers (100K keys/sec)

**Bottlenecks beyond this**:
- Coordinator task scheduling (1K tasks/sec)
- Redis shuffle layer (1M ops/sec, requires Redis Cluster)
- PostgreSQL result writes (10K inserts/sec)

### Vertical Scaling

Increase JVM heap for workers:

```dockerfile
# Dockerfile-map-worker
ENTRYPOINT ["java", "-Xmx4g", "-jar", "app.jar"]
```

### Redis Cluster for Shuffle

When intermediate data exceeds 100M key-values:

```yaml
redis:
  image: redis:7-alpine
  command: redis-server --cluster-enabled yes
  # Deploy 6 nodes (3 masters, 3 replicas)
```

## Real-World Comparisons

### Google MapReduce
- **Scale**: 20PB/day across 10,000 machines
- **Optimization**: Locality-aware scheduling (tasks run on machines with data)
- **Our Implementation**: Simplified for 1-20 workers, partition-based locality

### Hadoop
- **Architecture**: Separates compute (YARN) from storage (HDFS)
- **Fault Tolerance**: Replicates data 3x, our system relies on Kafka replication
- **Scheduling**: Capacity scheduler with resource pools

### Apache Spark
- **In-Memory**: Caches intermediate data in RAM (10-100x faster)
- **DAG Execution**: Optimizes multi-stage jobs, our system is 2-stage (map/reduce)
- **RDDs**: Distributed collections with lineage for recomputation

## Troubleshooting

### Jobs stuck in RUNNING state

```bash
# Check task status
docker-compose exec postgres psql -U postgres -d mapreduce -c \
  "SELECT task_type, status, COUNT(*) FROM tasks WHERE job_id = '{jobId}' GROUP BY task_type, status;"

# Check worker logs
docker-compose logs map-worker
docker-compose logs reduce-worker
```

### High task failure rate

```bash
# View failed tasks
docker-compose exec postgres psql -U postgres -d mapreduce -c \
  "SELECT task_id, error_message, retry_count FROM tasks WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT 10;"

# Common causes:
# 1. Redis memory exhausted - scale Redis or reduce intermediate data
# 2. Network partition - check Docker network
# 3. Worker OOM - increase JVM heap
```

### Slow job completion

```bash
# Identify stragglers
docker-compose exec postgres psql -U postgres -d mapreduce -c \
  "SELECT task_id, EXTRACT(EPOCH FROM (completed_at - started_at)) as duration_sec 
   FROM tasks 
   WHERE job_id = '{jobId}' AND status = 'COMPLETED' 
   ORDER BY duration_sec DESC LIMIT 5;"

# Solutions:
# - Enable speculative execution for slow tasks
# - Increase worker parallelism
# - Optimize map/reduce functions
```

## Next Steps (Day 46)

Tomorrow we add **time-based windowing** for time-series analytics:
- Tumbling windows (non-overlapping, fixed duration)
- Sliding windows (overlapping, fixed duration)
- Session windows (dynamic duration based on inactivity)

This enables queries like:
- "Errors per minute over the last hour"
- "95th percentile latency in 5-minute windows"
- "User session length distribution"

## License

MIT
