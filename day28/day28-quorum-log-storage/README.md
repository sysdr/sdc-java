# Day 28: Quorum-Based Log Storage System

A production-ready implementation of configurable consistency control using quorum-based replication. This system demonstrates the CAP theorem in action with tunable consistency levels (ONE, QUORUM, ALL).

## System Architecture

### Components

1. **Storage Nodes (5 replicas)**
   - Versioned key-value storage with vector clocks
   - PostgreSQL persistence per node
   - Tracks causality for conflict detection

2. **Quorum Coordinator**
   - Implements W+R>N consistency protocol
   - Manages replica selection and failure handling
   - Resolves conflicts using version vectors

3. **API Gateway**
   - Public REST API with consistency parameters
   - Client-facing interface for log operations

## Key Concepts

### Quorum Mathematics

For N replicas with W writes and R reads:
- **W + R > N**: Guarantees overlap between reads and writes
- **Example (N=5)**: W=3, R=3 ensures every read sees at least one latest write

### Consistency Levels

- **ONE**: Single replica (fast, low durability)
  - Write latency: ~5ms
  - Use case: High throughput, eventual consistency OK

- **QUORUM**: Majority of replicas (balanced)
  - Write latency: ~15ms
  - Use case: Most production workloads

- **ALL**: All replicas (slow, high durability)
  - Write latency: ~50ms
  - Use case: Critical data requiring strong consistency

### Version Vectors

Each value has a version vector tracking which nodes wrote it:
```
node-1: 5, node-2: 3, node-3: 8
```

When reading multiple versions:
- **Dominated**: One vector is strictly newer (return it)
- **Concurrent**: Vectors conflict (return all for resolution)

## Quick Start

### Prerequisites

- Docker and Docker Compose
- 8GB RAM minimum
- Ports 8080-8094, 5432-5436, 9090, 3000 available

### Deployment

```bash
# Build and start all services
docker-compose up --build -d

# Wait for services to be healthy (~2 minutes)
docker-compose ps

# Verify system health
curl http://localhost:8080/api/logs/health
```

### Basic Operations

**Write with different consistency levels:**

```bash
# Fast write (ONE replica)
curl -X POST "http://localhost:8080/api/logs?consistency=ONE" \
  -H "Content-Type: application/json" \
  -d '{"key":"user-123","value":"session-data"}'

# Balanced write (QUORUM)
curl -X POST "http://localhost:8080/api/logs?consistency=QUORUM" \
  -H "Content-Type: application/json" \
  -d '{"key":"user-456","value":"profile-update"}'

# Consistent write (ALL replicas)
curl -X POST "http://localhost:8080/api/logs?consistency=ALL" \
  -H "Content-Type: application/json" \
  -d '{"key":"payment-789","value":"transaction-data"}'
```

**Read with consistency control:**

```bash
# Fast read (might be stale)
curl "http://localhost:8080/api/logs/user-123?consistency=ONE"

# Balanced read
curl "http://localhost:8080/api/logs/user-456?consistency=QUORUM"

# Strongly consistent read
curl "http://localhost:8080/api/logs/payment-789?consistency=ALL"
```

## Running Tests

### Integration Tests

```bash
./integration-tests/test_quorum.sh
```

Tests all consistency levels and measures response times.

### Load Testing

```bash
./load-test.sh
```

Demonstrates latency trade-offs:
- ONE: 5-10ms average
- QUORUM: 15-30ms average
- ALL: 50-100ms average

### Failure Scenario Testing

```bash
./failure-test.sh
```

Simulates replica failures and shows:
- ONE: Always available
- QUORUM: Tolerates 2/5 failures
- ALL: Requires all replicas

## Monitoring

### Prometheus Metrics

Access at http://localhost:9090

Key metrics:
- `http_server_requests_seconds`: Request latencies by consistency level
- `quorum_write_success_rate`: Write success rate
- `version_vector_conflicts`: Conflict detection rate

### Grafana Dashboards

Access at http://localhost:3000 (admin/admin)

Pre-configured dashboards show:
- Latency distribution by consistency level
- Quorum success rates
- Replica health and availability

## Architecture Details

### Write Path

1. Client sends write to API Gateway with consistency level
2. Gateway forwards to Quorum Coordinator
3. Coordinator calculates replica set (5 nodes)
4. Sends parallel writes to all replicas
5. Waits for W acknowledgments (1, 3, or 5 based on level)
6. Returns success/failure to client

### Read Path

1. Client sends read to API Gateway with consistency level
2. Gateway forwards to Quorum Coordinator
3. Coordinator reads from R replicas (1, 3, or 5)
4. Compares version vectors to detect conflicts
5. Returns latest value or conflicts for resolution
6. Client receives result

### Conflict Resolution

When concurrent writes create conflicts:
1. Version vectors identify concurrent versions
2. Both/all concurrent values returned to client
3. Client chooses resolution strategy:
   - Last-write-wins (timestamp)
   - Application-specific merge
   - Manual resolution

## Production Considerations

### Scaling

Current: 5 replicas, N=5
To scale:
1. Add replica nodes to docker-compose
2. Update `quorum.replicas` in coordinator config
3. Recalculate QUORUM (N/2 + 1)

### Performance Tuning

**For read-heavy workloads:**
- Use R=1, W=5 (fast reads, slow writes)

**For write-heavy workloads:**
- Use R=5, W=1 (fast writes, slow reads)

**For balanced workloads:**
- Use R=3, W=3 (default QUORUM)

### Failure Modes

**Network partition:**
- With 5 replicas, ensure they span failure domains
- 3-2 split: Both sides might think they have quorum
- Solution: Rack awareness in replica placement

**Slow replica:**
- Circuit breaker prevents cascading failures
- After N timeouts, mark replica unhealthy
- Hedged requests to backup replicas

**Version vector growth:**
- Hot keys accumulate large vectors
- Monitor vector size
- Consider pruning or timestamp fallback

## Real-World Examples

### DynamoDB
- Uses quorums with N=3 across availability zones
- Eventually consistent reads: R=1 (fast, cheap)
- Strongly consistent reads: R=QUORUM (slower, accurate)

### Cassandra
- Configurable per-request consistency
- LOCAL_QUORUM: Quorum within datacenter
- EACH_QUORUM: Quorum in every datacenter

### Riak
- Exposes full vector clocks to applications
- Allows custom conflict resolution
- Sloppy quorums with hinted handoff

## Troubleshooting

**Writes timing out:**
- Check replica health: `docker-compose ps`
- Reduce W (use lower consistency level)
- Increase timeout in coordinator config

**Reading stale data:**
- Use higher consistency level (QUORUM or ALL)
- Check version vector conflicts
- Verify anti-entropy processes

**Conflicts detected:**
- Expected with concurrent writes
- Implement application-level resolution
- Consider last-write-wins for non-critical data

## Learning Objectives

After completing this lesson, you should understand:

1. **Quorum Mathematics**: How W+R>N provides consistency guarantees
2. **CAP Theorem**: Practical trade-offs between consistency and availability
3. **Version Vectors**: Tracking causality and detecting conflicts
4. **Consistency Tuning**: Choosing appropriate levels per operation
5. **Failure Handling**: Graceful degradation during partial failures

## Next Steps

Tomorrow: Anti-entropy mechanisms
- Merkle tree comparisons
- Read repair
- Hinted handoff replay
- Background reconciliation

## Clean Up

```bash
# Stop all services
docker-compose down

# Remove volumes (deletes all data)
docker-compose down -v
```

## Additional Resources

- [Amazon Dynamo Paper](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf)
- [Cassandra Architecture](https://cassandra.apache.org/doc/latest/architecture/)
- [Version Vectors Explained](https://en.wikipedia.org/wiki/Version_vector)
