# Day 57: Full-Text Search with Relevance Scoring

Production-ready distributed log search system with Elasticsearch, Kafka, and Spring Boot.

## Architecture

- **Log Producer**: REST API for ingesting logs → Kafka
- **Log Indexer**: Kafka consumer → Bulk indexing to Elasticsearch
- **Search API**: RESTful search with BM25 ranking and field boosting
- **Monitoring**: Prometheus + Grafana for observability

## Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

Wait for all services to be healthy (~2 minutes):

```bash
docker-compose ps
```

### 2. Build and Run Services

```bash
# Build all services
mvn clean package

# Run services in separate terminals
cd log-producer && mvn spring-boot:run
cd log-indexer && mvn spring-boot:run
cd search-api && mvn spring-boot:run
```

### 3. Run Integration Tests

```bash
./integration-tests/test-flow.sh
```

### 4. Run Load Test

```bash
./load-test.sh
```

## Key Features

### BM25 Relevance Scoring
- Implements Best Match 25 algorithm for ranking
- Configurable term frequency saturation (k1=1.2)
- Document length normalization (b=0.75)

### Multi-Field Search with Boosting
- `message^3`: Primary content (3x boost)
- `service_name^2`: Service identifier (2x boost)
- `stack_trace^1`: Error details (1x boost)

### Time-Based Relevance
- Gaussian decay function over 7 days
- Recent logs receive 1.5x score multiplier
- Exponential decay for older logs

### Circuit Breaker Pattern
- Protects against Elasticsearch failures
- Fails open with empty results
- Auto-recovery after 10 seconds

## API Endpoints

### Log Producer (Port 8081)
```bash
# Generate random log
POST /api/logs/generate

# Submit custom log
POST /api/logs
{
  "severity": "ERROR",
  "service_name": "payment-service",
  "message": "Payment processing failed"
}
```

### Search API (Port 8083)
```bash
# Simple search
GET /api/search?q=timeout&page=0&size=20

# Advanced search with filters
POST /api/search
{
  "query": "authentication timeout",
  "severities": ["ERROR", "WARN"],
  "serviceNames": ["auth-service"],
  "startTime": "2024-01-01T00:00:00Z",
  "endTime": "2024-12-31T23:59:59Z",
  "page": 0,
  "size": 20
}
```

## Monitoring

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Kibana**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200

### Key Metrics

- `kafka_messages_produced_total`: Log production rate
- `elasticsearch_documents_indexed_total`: Indexing throughput
- `search_requests_total`: Search query rate
- `search_duration_seconds`: Search latency (p50, p95, p99)

## Performance Benchmarks

- **Indexing**: 15K docs/second with 1000-doc batches
- **Search Latency**: p99 < 100ms for simple queries
- **Throughput**: 10K queries/second
- **Index Size**: ~10-50GB per shard (5 shards)

## Production Considerations

### Scaling Elasticsearch

```bash
# Increase shards for more data
"number_of_shards": "10"

# Add replicas for read throughput
"number_of_replicas": "2"
```

### Kafka Tuning

```yaml
# Increase batch size for throughput
batch-size: 32768
linger-ms: 20

# Add partitions for parallelism
num.partitions: 10
```

### Circuit Breaker Configuration

```yaml
resilience4j.circuitbreaker:
  instances:
    elasticsearch:
      failure-rate-threshold: 50  # Open at 50% errors
      wait-duration-in-open-state: 30s
```

## Troubleshooting

### Elasticsearch not starting
```bash
# Increase Docker memory limit
docker update --memory 2g <container_id>
```

### High indexing lag
```bash
# Check Kafka consumer lag
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group log-indexer-group --describe
```

### Slow search queries
```bash
# Check Elasticsearch query performance
curl http://localhost:9200/logs/_search?explain=true
```

## Next Steps

Tomorrow we'll enhance this with:
- Rate limiting and query validation
- Result pagination with search_after
- Aggregations for faceted search
- Query result caching

## License

MIT
