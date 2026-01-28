# Day 55: Faceted Search System

A production-grade distributed log processing system with multi-dimensional faceted search capabilities, enabling complex filtering across billions of log events.

## Architecture Overview

```
Log Events → Kafka → Elasticsearch (Inverted Indexes)
                  ↓
             Kafka Streams (Real-time Aggregation)
                  ↓
              Redis Cache ← → API Gateway ← → Users
```

### Components

1. **Log Producer** (Port 8081)
   - Generates logs with multiple faceted dimensions
   - Publishes to Kafka at 10 events/second
   - Supports burst generation for load testing

2. **Faceted Search Service** (Port 8082)
   - Elasticsearch integration with inverted indexes
   - Multi-dimensional filtering with boolean queries
   - Intelligent query planning and execution
   - Circuit breaker for resilience
   - Redis caching for repeated queries

3. **Aggregation Service** (Port 8083)
   - Real-time facet count computation via Kafka Streams
   - Maintains 5-minute windowed aggregations
   - Pre-computes counts for common dimensions
   - Updates Redis cache with fresh aggregations

4. **API Gateway** (Port 8080)
   - Unified API for log generation and search
   - Routes requests to appropriate services

## Quick Start

### Prerequisites
- Docker & Docker Compose
- 8GB RAM minimum
- curl and jq for testing

### Deploy the System

```bash
# Generate all project files
chmod +x setup.sh
./setup.sh

# Build and start services
cd day55-faceted-search-system
docker-compose up -d

# Wait for services to be ready (2-3 minutes)
docker-compose logs -f
```

### Verify Deployment

```bash
# Check service health
curl http://localhost:8080/api/health
curl http://localhost:8082/api/search/health

# Verify Elasticsearch
curl http://localhost:9200/_cluster/health

# Check Kafka topics
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --bootstrap-server localhost:9092 --list
```

## Usage Examples

### Generate Test Logs

```bash
# Generate 1000 logs with random facets
curl -X POST "http://localhost:8080/api/logs/generate?count=1000"

# Continuous generation runs automatically at 10/sec
```

### Simple Faceted Search

```bash
# Find all ERROR logs
curl -X POST http://localhost:8082/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "level": "ERROR"
    },
    "limit": 20
  }' | jq
```

**Response:**
```json
{
  "results": [...],
  "totalHits": 4235,
  "facets": {
    "service": {
      "auth-service": 1834,
      "api-service": 1245,
      "payment-service": 1156
    },
    "environment": {
      "prod": 3012,
      "staging": 923,
      "dev": 300
    },
    "region": {...}
  },
  "queryTimeMs": 45,
  "fromCache": false
}
```

### Multi-Dimensional Search

```bash
# ERROR logs from auth-service in production
curl -X POST http://localhost:8082/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "level": "ERROR",
      "service": "auth-service",
      "environment": "prod"
    },
    "limit": 50
  }' | jq
```

### Time-Based Filtering

```bash
# ERROR logs from last 15 minutes
FROM_TS=$(($(date +%s) * 1000 - 900000))
TO_TS=$(($(date +%s) * 1000))

curl -X POST http://localhost:8082/api/search \
  -H "Content-Type: application/json" \
  -d "{
    \"filters\": {
      \"level\": \"ERROR\"
    },
    \"fromTimestamp\": $FROM_TS,
    \"toTimestamp\": $TO_TS,
    \"limit\": 100
  }" | jq
```

### Complex Boolean Queries

```bash
# ERROR or WARN logs from specific regions
curl -X POST http://localhost:8082/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "region": "us-east-1"
    },
    "textQuery": "timeout OR connection",
    "limit": 50
  }' | jq '.facets.level'
```

## Testing

### Integration Tests

```bash
cd integration-tests
./test_faceted_search.sh
```

Tests verify:
- ✅ Log generation and ingestion
- ✅ Single-facet filtering
- ✅ Multi-dimensional queries
- ✅ Facet count accuracy
- ✅ Time-based filtering
- ✅ Cache performance

### Load Testing

```bash
# Test with 1000 requests, 50 concurrent
./load-test.sh

# Expected performance:
# - P50 latency: <100ms
# - P99 latency: <500ms
# - Throughput: 100+ req/sec
```

### Performance Benchmarks

**Query Patterns:**

| Query Type | Selectivity | Latency (P99) | Strategy |
|------------|-------------|---------------|----------|
| Single facet (level=ERROR) | 5% | 45ms | Filter-first |
| Multi-facet (3+ filters) | 0.5% | 32ms | Filter-first |
| Broad search (level!=ERROR) | 95% | 280ms | Aggregate-first |
| Time window (15min) | 10% | 67ms | Time-index scan |

**Cache Hit Rates:**
- Dashboard queries (repeated filters): 82%
- Exploration queries (new filters): 15%
- Overall: ~70%

## Monitoring

### Prometheus Metrics

Access at http://localhost:9090

Key metrics to monitor:
```promql
# Query latency
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{uri="/api/search"}[5m]))

# Cache hit rate
rate(cache_hits_total[5m]) / rate(cache_requests_total[5m])

# Elasticsearch heap usage
elasticsearch_jvm_memory_used_bytes / elasticsearch_jvm_memory_max_bytes

# Kafka consumer lag
kafka_consumer_lag_sum
```

### Grafana Dashboards

Access at http://localhost:3000 (admin/admin)

Pre-configured dashboards:
1. **Search Performance**: Query latency, throughput, error rates
2. **Facet Analytics**: Most queried dimensions, cardinality trends
3. **Cache Efficiency**: Hit rates, eviction rates, memory usage
4. **Infrastructure**: Elasticsearch cluster health, Kafka throughput

## Architecture Patterns

### 1. Inverted Indexes

Elasticsearch maintains inverted indexes for each faceted field:
```
Field: service
  auth-service → [doc1, doc5, doc89, ...]
  api-service  → [doc2, doc12, doc45, ...]
```

**Trade-offs:**
- 30-40% storage overhead
- O(k) query time vs O(n) full scan
- Write amplification (multiple index updates per log)

### 2. Bitmap Intersection

Multi-facet queries use compressed bitmaps for fast intersection:
```java
Bitmap serviceFilter = getBitmap("service", "auth-service");
Bitmap levelFilter = getBitmap("level", "ERROR");
Bitmap result = serviceFilter.and(levelFilter);  // Microseconds
```

### 3. Distributed Aggregation

Scatter-gather pattern across Elasticsearch shards:
1. Coordinator sends query to all shards
2. Each shard computes local top-K facets
3. Coordinator merges and re-ranks results

**Network efficiency:** 200× reduction in data transfer

### 4. Time-Bucketed Caching

Cache keys include time buckets for natural expiration:
```
cache_key = "search:level=ERROR:2024-01-28T10:00"
TTL = 60 seconds
```

Cache hit rate: 70-80% for dashboard queries

## Production Considerations

### Capacity Planning

**Per 1M logs/day:**
- Elasticsearch: 2GB storage + 1GB indexes
- Redis: 100MB cache
- Kafka: 500MB retention (1 day)

**For 1B logs/day (Datadog scale):**
- 500+ Elasticsearch nodes
- 100+ Redis cluster nodes
- Hot/warm/cold storage tiers
- Distributed aggregation coordinators

### Failure Scenarios

1. **Elasticsearch Shard Unavailable**
   - Returns partial results with warning
   - Circuit breaker prevents cascade failures
   - Fallback to other shards

2. **Redis Cache Failure**
   - Degrades to direct Elasticsearch queries
   - Increased latency (2-3x)
   - No data loss

3. **Kafka Consumer Lag**
   - Indexing delay increases
   - Facet counts become stale
   - Auto-scaling consumers

### Security

- [ ] Enable Elasticsearch security features
- [ ] Add API authentication (OAuth2/JWT)
- [ ] Implement rate limiting
- [ ] Encrypt Kafka messages
- [ ] Add audit logging

## Troubleshooting

### Issue: High Query Latency

```bash
# Check Elasticsearch heap
curl http://localhost:9200/_nodes/stats/jvm

# Check cache hit rate
redis-cli INFO stats

# Analyze slow queries
curl http://localhost:9200/_nodes/stats/indices/search
```

**Solutions:**
- Increase Elasticsearch heap
- Add more shards for parallelism
- Optimize high-cardinality fields
- Pre-compute common aggregations

### Issue: Cache Misses

```bash
# Check Redis memory usage
redis-cli INFO memory

# Inspect cached keys
redis-cli KEYS "search:*"
```

**Solutions:**
- Increase cache TTL
- Implement cache warming
- Pre-fetch common facet combinations

### Issue: Indexing Lag

```bash
# Check Kafka consumer lag
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group faceted-search-group
```

**Solutions:**
- Scale Elasticsearch nodes
- Increase bulk indexing batch size
- Add more Kafka partitions

## Scaling Strategy

### Horizontal Scaling

**Elasticsearch:**
```bash
# Add data nodes
docker-compose up --scale elasticsearch=3

# Rebalance shards
curl -X POST "localhost:9200/_cluster/reroute"
```

**Kafka Consumers:**
```bash
# Scale search service
docker-compose up --scale faceted-search-service=3

# Partitions distribute load automatically
```

### Vertical Scaling

- Increase Elasticsearch heap: 4GB → 16GB
- Add more Kafka partitions: 3 → 12
- Increase Redis max memory: 256MB → 2GB

## Comparison to Production Systems

| System | Scale | Key Patterns |
|--------|-------|--------------|
| **Datadog** | 1T logs/day | 500+ ES nodes, frozen tier for archives |
| **Splunk** | 100K queries/sec | Bitmap indexes, distributed aggregation |
| **Elastic Cloud** | 500B docs | Hot/warm/cold architecture, cluster coordination |
| **This Project** | 10K logs/sec | Same core patterns, educational scale |

## Next Steps

1. **Real-time Indexing** (Day 56)
   - Sub-second indexing latency
   - Near real-time search capabilities
   - Refresh interval optimization

2. **Advanced Optimizations**
   - HyperLogLog for approximate counts
   - Adaptive query planning
   - Multi-level caching

3. **Production Hardening**
   - Multi-datacenter replication
   - Disaster recovery
   - Compliance & data retention

## Resources

- [Lesson Article](lesson_article.md) - Deep dive into distributed search patterns
- [Elasticsearch Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Kafka Streams](https://kafka.apache.org/documentation/streams/)
- [System Design Interview](https://github.com/donnemartin/system-design-primer)

## Architecture Diagram

See `system_architecture.svg` for visual representation of:
- Service topology and data flows
- Inverted index structure
- Scatter-gather aggregation
- Cache layers and strategies
- Failure points and circuit breakers

---

**Course:** 254-Day System Design Course  
**Module:** 2 - Scalable Log Processing  
**Week:** 8 - Distributed Log Analytics  
**Day:** 55 - Faceted Search Capabilities
