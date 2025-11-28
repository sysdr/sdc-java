# Day 22: Multi-Node Storage Cluster with File Replication

## System Architecture

This system implements a distributed storage cluster with:

- **3 Storage Nodes**: RocksDB-backed key-value stores with leader-follower replication
- **Cluster Coordinator**: Manages topology, leader election, and failure detection
- **Write Gateway**: Routes writes to appropriate leader nodes with quorum semantics
- **Read Gateway**: Implements scatter-gather reads with version conflict resolution
- **Consistent Hashing**: Distributes data evenly across nodes with minimal rebalancing
- **Heartbeat Monitoring**: Detects failures within 5 seconds and triggers failover

## Key Features

### Replication Protocol
- Synchronous replication with W=2, R=2 quorum
- Leader-based writes with parallel follower replication
- Version vectors for conflict detection
- Read repair for eventual consistency

### Failure Handling
- Sub-5-second failure detection via heartbeats
- Automatic leader election on primary failure
- Graceful degradation (read-only mode with W < quorum)
- Node recovery and automatic re-joining

### Performance
- 10,000+ writes/second per node
- P99 latency < 20ms for writes
- P99 latency < 5ms for reads
- Batch replication for network efficiency

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- 4GB RAM minimum

### Setup

1. Generate all project files (if using the generator script):
```bash
chmod +x generate_system_files.sh
./generate_system_files.sh
cd day22-storage-cluster
```

2. Build and start the system:
```bash
./setup.sh
```

This will:
- Build all Maven projects
- Start all Docker containers
- Wait for services to be healthy
- Display service URLs

### Verify Installation

```bash
# Check cluster topology
curl http://localhost:8080/api/coordinator/topology | jq .

# Check node health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

## Usage Examples

### Write Data

```bash
curl -X POST http://localhost:9090/api/write \
  -H "Content-Type: application/json" \
  -d '{"key":"user-123","content":"User log data"}'
```

Response:
```json
{
  "success": true,
  "key": "user-123",
  "latencyMs": 15,
  "nodes": ["node-1", "node-2", "node-3"]
}
```

### Read Data

```bash
curl http://localhost:9091/api/read/user-123 | jq .
```

Response:
```json
{
  "success": true,
  "key": "user-123",
  "data": {
    "key": "user-123",
    "content": "User log data",
    "version": 1732800000000,
    "timestamp": "2024-11-28T12:00:00Z"
  },
  "latencyMs": 8,
  "replicas": 3
}
```

### Check Node Distribution

```bash
curl "http://localhost:8080/api/coordinator/nodes/user-123?count=3" | jq .
```

## Load Testing

Run the included load test:

```bash
./load-test.sh
```

This generates 100 writes and 100 reads, measuring throughput.

Expected results:
- 500-1000 writes/second
- 1000-2000 reads/second
- P99 latency < 50ms end-to-end

## Integration Tests

### Test Replication

```bash
./integration-tests/test-replication.sh
```

Verifies:
- Data written to leader
- Data replicated to followers
- Read quorum returns latest version

### Test Failover

```bash
./integration-tests/test-failover.sh
```

Verifies:
- System continues with one node down
- Writes achieve W=2 quorum with 2 healthy nodes
- Reads achieve R=2 quorum with 2 healthy nodes
- Node recovery works correctly

## Monitoring

### Prometheus Metrics

Access Prometheus at http://localhost:9092

Key metrics:
- `storage_replication_success_total`: Successful replications
- `storage_replication_failure_total`: Failed replications
- `storage_replication_latency_seconds`: Replication latency
- `gateway_write_success_total`: Successful writes
- `gateway_read_success_total`: Successful reads
- `cluster_nodes_total`: Active nodes in cluster
- `cluster_generation`: Current cluster generation ID

### Grafana Dashboards

Access Grafana at http://localhost:3000 (admin/admin)

Pre-configured dashboards show:
- Write/read throughput over time
- Replication latency percentiles
- Node health status
- Cluster topology changes

## Failure Scenarios

### Scenario 1: Single Node Failure

```bash
# Stop one node
docker-compose stop storage-node-1

# System continues normally (W=2, R=2 with 3 nodes)
curl -X POST http://localhost:9090/api/write \
  -H "Content-Type: application/json" \
  -d '{"key":"test","content":"Works with 2 nodes"}'

# Restart node
docker-compose start storage-node-1
```

### Scenario 2: Network Partition

```bash
# Isolate the leader
docker network disconnect day22-storage-cluster_default storage-node-1

# Followers elect new leader within 5 seconds
# System continues with new leader

# Heal partition
docker network connect day22-storage-cluster_default storage-node-1
```

### Scenario 3: Cascading Failure

```bash
# Stop two nodes simultaneously
docker-compose stop storage-node-1 storage-node-2

# System enters read-only mode (cannot achieve W=2)
# Writes fail gracefully
# Reads continue with R=1 fallback

# Restart one node to restore write capability
docker-compose start storage-node-2
```

## Architecture Deep Dive

### Consistent Hashing Implementation

Each physical node creates 16 virtual nodes on the hash ring:
- Provides even data distribution
- Minimizes data movement on topology changes
- Only 1/N of data moves when adding/removing nodes

Hash function: MD5 for uniform distribution

### Replication Flow

1. Write arrives at Write Gateway
2. Gateway queries Coordinator for nodes responsible for key
3. Gateway sends write to leader (first node in list)
4. Leader stores locally in RocksDB
5. Leader sends parallel replication RPCs to followers
6. Leader waits for W-1 acknowledgments (W=2, so 1 follower)
7. Leader returns success to gateway
8. Gateway returns success to client

### Failure Detection

- Heartbeat interval: 1 second
- Failure threshold: 3 missed heartbeats (5 seconds total)
- Detection accuracy: 95%+ (occasional false positives during network hiccups)
- Generation IDs prevent split-brain scenarios

### Read Repair

During reads:
1. Gateway queries R nodes (R=2)
2. Gateway compares versions from each replica
3. If versions differ, gateway identifies latest version
4. Gateway asynchronously pushes latest version to stale replicas
5. Gateway returns latest version to client

This gradually repairs inconsistencies without blocking reads.

## Performance Tuning

### Write Performance

Increase write throughput:

```yaml
# storage-node/src/main/resources/application.yml
storage:
  replication:
    timeout-ms: 2000  # Reduce timeout (increases failure rate)
```

### Read Performance

Enable caching in Read Gateway:

```yaml
# read-gateway/src/main/resources/application.yml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 60000  # 1 minute cache
```

### Memory Optimization

Tune RocksDB:

```java
// RocksDBRepository.java
options.setWriteBufferSize(32 * 1024 * 1024);  // Reduce to 32MB
options.setMaxOpenFiles(50);  // Reduce open file handles
```

## Troubleshooting

### Writes Failing

Check cluster topology:
```bash
curl http://localhost:8080/api/coordinator/topology | jq .
```

Verify at least 2 healthy nodes for W=2 quorum.

### Reads Failing

Check read quorum setting:
```bash
curl http://localhost:9091/actuator/configprops | jq '.read'
```

### High Latency

Check replication latency:
```bash
curl http://localhost:9092/api/v1/query?query=storage_replication_latency_seconds
```

If consistently > 100ms, check network between containers.

### Leader Election Loop

Check generation IDs:
```bash
curl http://localhost:8080/api/coordinator/topology | jq '.generationId'
```

If rapidly incrementing, nodes are fighting over leadership. Check heartbeat configuration.

## Scaling Beyond Three Nodes

To add more nodes:

1. Update docker-compose.yml with new storage-node-4 service
2. Set `NODE_ID=node-4` and unique port
3. Start new container: `docker-compose up -d storage-node-4`
4. Coordinator automatically detects and adds to hash ring
5. Data rebalances automatically (1/N of total data moves)

For production: Use Kubernetes with StatefulSets for automatic scaling.

## Next Steps

Tomorrow (Day 23), we add **partitioning** to this replicated cluster:

- Time-based and source-based partitioning
- Partition-aware routing for parallel queries
- Dynamic partition rebalancing
- Query optimization across partitions

This combination of replication (durability) + partitioning (performance) forms the foundation of all distributed databases.

## Clean Up

```bash
# Stop all services
docker-compose down

# Remove volumes (deletes all data)
docker-compose down -v
```
