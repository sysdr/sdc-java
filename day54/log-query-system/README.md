# Day 54: Query Language for Complex Log Searches

## Architecture Overview

This system implements a distributed SQL-like query language for log analytics:

```
┌─────────────────┐
│  Query Request  │
└────────┬────────┘
         │
    ┌────▼────────────────┐
    │ Query Coordinator   │ ← Parse, Optimize, Coordinate
    │  (Port 8080)        │
    └────┬────────────────┘
         │
    ┌────▼─────────────────────────┐
    │  Distributed Execution       │
    ├──────────┬──────────┬────────┤
    │ Exec-1   │ Exec-2   │ Exec-3 │ ← Parallel Query Execution
    │ :8081    │ :8082    │ :8083  │
    └──────────┴──────────┴────────┘
         │          │          │
    ┌────▼──────────▼──────────▼───┐
    │      PostgreSQL (Index)      │ ← Indexed Log Storage
    └──────────────────────────────┘
```

## Key Features

### 1. ANTLR-Based Query Parser
- SQL-like syntax: `SELECT * WHERE level='ERROR' AND service='api' LIMIT 100`
- Support for WHERE, GROUP BY, ORDER BY, LIMIT
- Aggregation functions: COUNT, SUM, AVG, MIN, MAX

### 2. Query Optimizer
- Cost-based optimization
- Index selection based on selectivity
- Predicate pushdown to executor nodes
- Query result caching with Redis

### 3. Distributed Execution
- Coordinator-worker pattern
- Parallel query execution across 3 nodes
- Merge-sort for ordered results
- Circuit breaker for fault tolerance

### 4. Performance Features
- Result streaming with chunked transfer
- Multi-level caching (Redis)
- Projection pruning (fetch only needed fields)
- Index-aware query plans

## Quick Start

### 1. Start the System

```bash
# Build and start all services
docker-compose up -d

# Wait for services to be ready (~60 seconds)
docker-compose logs -f query-coordinator
```

### 2. Verify System Health

```bash
# Check coordinator
curl http://localhost:8080/api/query/health

# Check executors
curl http://localhost:8081/api/health
curl http://localhost:8082/api/health
curl http://localhost:8083/api/health
```

### 3. Execute Queries

```bash
# Simple query
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT * WHERE level = '\''ERROR'\'' LIMIT 10"}'

# Complex query with multiple conditions
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT service, level WHERE level = '\''ERROR'\'' AND service = '\''api-gateway'\'' LIMIT 50"}'

# Test cache (repeat same query)
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT * WHERE level = '\''ERROR'\'' LIMIT 10"}'
```

### 4. Run Integration Tests

```bash
cd integration-tests
./test-queries.sh
```

### 5. Load Testing

```bash
# Generate 100 concurrent queries
./load-test.sh

# Monitor in Grafana
open http://localhost:3000
# Login: admin/admin
```

## Query Syntax

### Basic Query
```sql
SELECT * WHERE level = 'ERROR' LIMIT 100
```

### Field Projection
```sql
SELECT service, level, message WHERE level = 'ERROR'
```

### Multiple Conditions
```sql
SELECT * WHERE level = 'ERROR' AND service = 'api-gateway'
```

### Operators Supported
- Equality: `=`, `!=`
- Comparison: `>`, `<`, `>=`, `<=`
- Pattern: `LIKE` (e.g., `message LIKE 'exception'`)

## System Components

### Query Coordinator (Port 8080)
- Receives and parses queries using ANTLR
- Optimizes query plans (index selection, predicate ordering)
- Coordinates distributed execution across executor nodes
- Aggregates results from multiple partitions
- Manages Redis cache for query results

### Query Executor (Ports 8081-8083)
- Executes node-local queries against PostgreSQL
- Consumes logs from Kafka for indexing
- Maintains local indexes (B-tree, inverted)
- Implements predicate pushdown
- Returns partial results to coordinator

### Log Producer (Port 8082)
- Generates synthetic log events
- Publishes to Kafka topic `log-events`
- Simulates 10 logs/second per service

## Performance Characteristics

### Query Latency (P95)
- Simple indexed query: 50-100ms
- Complex multi-predicate: 100-200ms
- Cache hit: 5-10ms

### Throughput
- 200+ queries/second (cached)
- 50-100 queries/second (uncached)

### Scalability
- Horizontal: Add more executor nodes
- Vertical: Increase PostgreSQL resources

## Monitoring

### Prometheus Metrics (Port 9090)
- Query execution time
- Cache hit rate
- Node health status
- Kafka consumer lag

### Grafana Dashboards (Port 3000)
- Query throughput over time
- P50/P95/P99 latency
- Cache performance
- Executor node status

## Architecture Decisions

### Why ANTLR for Parsing?
- Production-grade parser with error recovery
- Extensible grammar (easy to add new syntax)
- Generated code is efficient and well-tested

### Why Coordinator-Worker Pattern?
- Decouples query planning from execution
- Enables parallel execution across partitions
- Coordinator can optimize without executor knowledge

### Why Predicate Pushdown?
- Minimizes network transfer (10-1000x reduction)
- Leverages executor-local indexes
- Reduces coordinator memory pressure

### Why Redis Caching?
- Dashboard queries are highly repetitive
- Cache invalidation is straightforward (TTL-based)
- 100x speedup for cached queries

## Troubleshooting

### Queries Timing Out
- Check executor node health: `docker-compose ps`
- Verify Kafka connectivity: `docker-compose logs kafka`
- Increase timeout in coordinator config

### High Memory Usage
- Reduce result set size with LIMIT
- Enable projection pruning (SELECT specific fields)
- Increase executor node resources

### Cache Not Working
- Verify Redis connection: `docker-compose logs redis`
- Check cache hit rate in Prometheus
- Ensure identical query strings (whitespace matters)

## Production Considerations

### Security
- Add authentication to query endpoint
- Implement query complexity limits
- Rate limit per user/tenant

### Scalability
- Partition logs by time range
- Use read replicas for PostgreSQL
- Implement query result pagination

### Reliability
- Add backup executor nodes
- Implement speculative execution
- Store query audit logs

## Next Steps

1. **Day 55**: Implement faceted search with dimension filtering
2. Add support for JOIN operations
3. Implement real-time query over streaming data
4. Add query result persistence (materialized views)

## Clean Up

```bash
# Stop all services
docker-compose down

# Remove volumes
docker-compose down -v
```

## References

- ANTLR Documentation: https://www.antlr.org/
- Dremel Paper: https://research.google/pubs/pub36632/
- Query Optimization: Database Systems - The Complete Book
