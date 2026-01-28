# Day 53: Distributed Indexing Across Multiple Nodes

## Overview

This project implements a production-grade distributed search index for log processing, demonstrating key patterns used by systems like Elasticsearch. The system uses consistent hashing to partition logs across multiple index nodes, scatter-gather queries to search all shards in parallel, and coordinated result merging.

## Architecture

```
                    ┌─────────────────┐
                    │  Log Producer   │
                    │  (Generates)    │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Shard Router   │
                    │ (Consistent Hash)│
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
    ┌──────────┐       ┌──────────┐      ┌──────────┐
    │ Index    │       │ Index    │      │ Index    │
    │ Node 1   │       │ Node 2   │      │ Node 3   │
    │ (Lucene) │       │ (Lucene) │      │ (Lucene) │
    └──────────┘       └──────────┘      └──────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │     Query       │
                    │  Coordinator    │
                    │ (Scatter-Gather)│
                    └─────────────────┘
```

### Components

1. **Index Nodes (3 instances)**: Store and search log data using Apache Lucene
2. **Shard Router**: Routes writes to appropriate nodes using consistent hashing
3. **Query Coordinator**: Executes scatter-gather queries across all shards
4. **Log Producer**: Generates test log data for the system
5. **Monitoring**: Prometheus + Grafana for observability

## Key Patterns Implemented

### 1. Consistent Hashing with Virtual Nodes
- 16 virtual nodes per physical node for even distribution
- MD5 hashing for key-to-node mapping
- Graceful handling of node additions/removals

### 2. Scatter-Gather Query Pattern
- Parallel queries to all index nodes (500ms timeout)
- Top-K merge algorithm using min-heap
- Partial result handling for fault tolerance

### 3. Distributed Indexing
- Apache Lucene for full-text search
- Periodic flush (10K docs or 30s)
- In-memory indexing with configurable persistence

## Quick Start

### Prerequisites
- Docker and Docker Compose
- 8GB+ RAM recommended
- Ports 8080-8083, 8090, 8095, 3000, 9090 available

### Start the System

```bash
# Make scripts executable
chmod +x start.sh load-test.sh integration-tests/test-distributed-indexing.sh

# Start all services
./start.sh
```

Wait ~1 minute for:
- Services to become healthy
- Log producer to generate initial data
- Indexing to complete

### Verify the System

```bash
# Run integration tests
./integration-tests/test-distributed-indexing.sh

# Expected output:
# - Hash ring distribution (should be ~33% per node)
# - 100 test logs indexed
# - Search results across all shards
# - Individual node statistics
```

## Testing

### Integration Tests

```bash
./integration-tests/test-distributed-indexing.sh
```

Validates:
- Hash ring distribution balance
- Write path through router to nodes
- Read path through coordinator scatter-gather
- Individual node health and stats

### Load Tests

```bash
./load-test.sh
```

Generates:
- 60 seconds of write load (50 concurrent writers)
- ~3000-5000 logs total
- 50 concurrent read queries
- Performance metrics

Expected results:
- Write throughput: 50-100 logs/sec per node
- Query latency: p99 < 100ms
- Even distribution across nodes (±10%)

### Manual Testing

```bash
# Search across all shards
curl 'http://localhost:8080/api/search?q=error&limit=10' | jq '.'

# Check hash ring distribution
curl 'http://localhost:8090/api/ring/distribution?samples=10000' | jq '.'

# Check individual node stats
curl 'http://localhost:8081/api/stats' | jq '.'
curl 'http://localhost:8082/api/stats' | jq '.'
curl 'http://localhost:8083/api/stats' | jq '.'

# Send a specific log through the router
curl -X POST http://localhost:8090/api/route \
  -H "Content-Type: application/json" \
  -d '{
    "logId": "manual_test_1",
    "tenantId": "tenant_1",
    "timestamp": 1234567890000,
    "level": "ERROR",
    "message": "Manual test error message",
    "service": "test-service"
  }'

# Search for the manual test log
curl 'http://localhost:8080/api/search?q=manual&limit=10' | jq '.'
```

## Monitoring

### Prometheus (http://localhost:9090)

Key queries:
```promql
# Indexing rate per node
rate(index_documents_indexed_total[1m])

# Query latency p99
histogram_quantile(0.99, rate(search_operation_duration_seconds_bucket[1m]))

# Scatter-gather performance
rate(coordinator_scatter_gather_duration_seconds_sum[1m]) / 
rate(coordinator_scatter_gather_duration_seconds_count[1m])

# Router throughput
rate(router_requests_routed_total[1m])
```

### Grafana (http://localhost:3000)

Login: admin/admin

Pre-configured dashboard shows:
- Documents indexed per node
- Search query latency (p50, p99)
- Scatter-gather timing
- Router distribution

## API Reference

### Query Coordinator (Port 8080)

**Search logs across all shards**
```bash
GET /api/search?q={query}&limit={limit}

Response:
{
  "logs": [/* LogEntry objects */],
  "totalHits": 250,
  "searchTimeMs": 45,
  "shardsQueried": 3,
  "shardsSucceeded": 3,
  "partial": false
}
```

### Shard Router (Port 8090)

**Route log to appropriate shard**
```bash
POST /api/route
Body: LogEntry

Response: "Indexed: log_123"
```

**Check hash ring distribution**
```bash
GET /api/ring/distribution?samples={count}

Response:
{
  "http://index-node-1:8081": 3345,
  "http://index-node-2:8082": 3312,
  "http://index-node-3:8083": 3343
}
```

### Index Nodes (Ports 8081-8083)

**Index a log directly**
```bash
POST /api/index
Body: LogEntry
```

**Search local shard**
```bash
GET /api/search?q={query}&limit={limit}
```

**Get node statistics**
```bash
GET /api/stats

Response:
{
  "nodeId": "node-1",
  "numDocs": 850,
  "maxDoc": 850,
  "pendingDocs": 0
}
```

## Performance Characteristics

### Throughput
- **Write**: 15-20K logs/sec per node (45-60K aggregate)
- **Query**: 50-100 queries/sec coordinator capacity
- **Latency**: p50=20ms, p99=100ms for 1M indexed logs

### Scalability
- Linear write scaling with nodes
- Query latency decreases with more nodes (better parallelism)
- Coordinator becomes bottleneck at ~10 nodes

### Consistency
- Eventually consistent (async indexing)
- Query sees data within 30 seconds of write
- No cross-shard transactions

## Production Considerations

### What to Change for Production

1. **Persistent Storage**: Replace `ByteBuffersDirectory` with `FSDirectory`
2. **Replication**: Add replica shards for fault tolerance
3. **Coordination**: Use ZooKeeper/etcd for cluster membership
4. **Security**: Add authentication, TLS, and authorization
5. **Resource Limits**: Configure heap sizes, file descriptors
6. **Backup**: Implement snapshot and restore procedures

### Monitoring Alerts

Set up alerts for:
- Node health check failures
- Shard imbalance >20%
- Query p99 >500ms
- Write failure rate >5%
- Disk usage >80%

### Capacity Planning

Rule of thumb:
- 1 node per 100GB of indexed data
- 1 replica per shard for 2-nines availability
- 3 replicas per shard for 4-nines availability
- 50% RAM headroom for query caching

## System Design Interview Talking Points

### Why Consistent Hashing?
- **vs Modulo hashing**: Only rehashes 1/N data on node changes
- **Virtual nodes**: Ensures even distribution despite heterogeneous hardware
- **Trade-off**: More complex than static partitioning

### Why Scatter-Gather?
- **Parallel queries**: Latency = slowest shard, not sum of all
- **Fault tolerance**: Partial results better than complete failure
- **Trade-off**: Coordinator becomes single point of bottleneck

### CAP Theorem Position
- **Chose**: Availability + Partition Tolerance (AP)
- **Sacrificed**: Strong consistency (eventual consistency instead)
- **Rationale**: Log search can tolerate stale data; uptime is critical

### Scaling Strategy
- **Write scaling**: Add more index nodes
- **Read scaling**: Add replicas, cache frequently accessed data
- **Bottleneck**: Coordinator at ~10 nodes → shard coordinator by key range

## Troubleshooting

### Issue: Services won't start
```bash
# Check Docker resources
docker stats

# Verify ports are free
netstat -an | grep -E '808[0-3]|8090|8095|3000|9090'

# Check logs
docker-compose logs -f
```

### Issue: Queries return no results
```bash
# Verify logs are being produced
curl http://localhost:8095/actuator/metrics/producer.logs.generated

# Check if logs reached index nodes
curl http://localhost:8081/api/stats

# Wait for indexing flush (up to 30 seconds)
```

### Issue: Unbalanced shard distribution
```bash
# Check hash ring
curl http://localhost:8090/api/ring/distribution?samples=10000

# Should be within ±10% of even (3333 per node for 10K samples)
# If imbalanced, increase virtual nodes in application.yml
```

## Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (clears all data)
docker-compose down -v

# Remove built images
docker-compose down --rmi all
```

## Next Steps

After completing this lesson:
1. ✅ Understand consistent hashing for data partitioning
2. ✅ Implement scatter-gather query patterns
3. ✅ Build fault-tolerant distributed systems
4. 📚 Tomorrow: Implement SQL-like query language for complex searches

## References

- Apache Lucene Documentation: https://lucene.apache.org/
- Elasticsearch Distributed Search: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request.html
- Consistent Hashing Paper: https://www.akamai.com/es/es/multimedia/documents/technical-publication/consistent-hashing-and-random-trees-distributed-caching-protocols-for-relieving-hot-spots-on-the-world-wide-web-technical-publication.pdf
