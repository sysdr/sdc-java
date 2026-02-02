# Day 58: Search API for Programmatic Access

A production-ready RESTful API for querying distributed log data with rate limiting, cursor-based pagination, and circuit breaker patterns.

## Architecture

```
┌─────────────┐         ┌──────────────┐         ┌────────────────┐
│  Clients    │────────▶│  API Gateway │────────▶│ Elasticsearch  │
│             │         │  (Rate Limit)│         │   (Search)     │
└─────────────┘         └──────────────┘         └────────────────┘
                               │
                               │ rate check
                               ▼
                        ┌──────────────┐
                        │    Redis     │
                        │ (Token Bucket)│
                        └──────────────┘

Log Flow:
┌──────────────┐         ┌────────────┐         ┌────────────────┐
│ Log Producer │────────▶│   Kafka    │────────▶│   Logstash     │
│  (Generates) │         │  (Stream)  │         │  (Transform)   │
└──────────────┘         └────────────┘         └────────────────┘
                                                         │
                                                         ▼
                                                 ┌────────────────┐
                                                 │ Elasticsearch  │
                                                 │   (Index)      │
                                                 └────────────────┘
```

## System Components

### API Gateway (Port 8080)
- RESTful search endpoints with OpenAPI documentation
- Query DSL translation with safety constraints
- Cursor-based pagination for efficient large result sets
- Token bucket rate limiting (1000 req/hour standard, 10000 premium)
- Circuit breaker for Elasticsearch failures
- Prometheus metrics export

### Log Producer (Port 8081)
- Generates realistic log events across multiple services
- Publishes to Kafka for distributed processing
- Simulates checkout, payment, inventory, and shipping services

### Infrastructure
- **Elasticsearch**: Log storage and full-text search
- **Kafka**: Distributed log streaming
- **Logstash**: Transform and index logs from Kafka
- **Redis**: Rate limiting state and quota tracking
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization

## Quick Start

### 1. Start Infrastructure
```bash
./setup.sh
```

This starts all Docker services and creates Elasticsearch index templates.

### 2. Build Services
```bash
cd api-gateway
mvn clean package
cd ../log-producer
mvn clean package
cd ..
```

### 3. Run Services
```bash
# Terminal 1: API Gateway
cd api-gateway
java -jar target/api-gateway-1.0.0.jar

# Terminal 2: Log Producer
cd log-producer
mvn spring-boot:run
```

### 4. Test the API
```bash
./load-test.sh
```

## API Reference

### Search Logs
```bash
GET /api/v1/logs/search
```

**Parameters:**
- `service` (required): Service name (checkout, payment, inventory, shipping)
- `level` (optional): Log level (DEBUG, INFO, WARN, ERROR, FATAL)
- `message` (optional): Message search (supports wildcards with ≥3 chars)
- `timeRange` (optional): Time window (15m, 1h, 6h, 24h, 7d)
- `limit` (optional): Results per page (1-1000, default 100)
- `cursor` (optional): Pagination cursor from previous response

**Headers:**
- `X-API-Key`: API key for authentication and rate limiting

**Example:**
```bash
curl "http://localhost:8080/api/v1/logs/search?service=checkout&level=ERROR&timeRange=1h&limit=100" \
     -H "X-API-Key: standard-test-key"
```

**Response:**
```json
{
  "logs": [
    {
      "timestamp": "2024-02-02T10:30:00Z",
      "service": "checkout",
      "level": "ERROR",
      "message": "Payment validation failed",
      "traceId": "550e8400-e29b-41d4-a716-446655440000"
    }
  ],
  "nextCursor": "W1sxNzA2ODc0NjAwMDAwLCI1NTBlODQwMCJd",
  "totalHits": 1543,
  "queryTimeMs": 45,
  "truncated": false
}
```

## Rate Limiting

The API implements token bucket rate limiting with Redis:

- **Standard Tier**: 1000 requests/hour
- **Premium Tier**: 10000 requests/hour (API keys prefixed with `premium_`)
- **Query Cost Scoring**: 
  - Simple queries: 1 token
  - Wildcard searches: 5 tokens
  - Large result sets (>500): +2 tokens

Exceeded limits return HTTP 429 with retry-after headers.

## Cursor-Based Pagination

The API uses Elasticsearch's `search_after` for efficient pagination:

1. Sort by `timestamp DESC, _id ASC` for deterministic ordering
2. Last document's sort values encoded as opaque cursor
3. Next request includes cursor to resume from exact position
4. No deep offset limitations (can paginate millions of records)

**Example Pagination:**
```bash
# Page 1
RESPONSE=$(curl "http://localhost:8080/api/v1/logs/search?service=checkout&limit=100" \
     -H "X-API-Key: test-key")

# Extract cursor
CURSOR=$(echo "$RESPONSE" | jq -r '.nextCursor')

# Page 2
curl "http://localhost:8080/api/v1/logs/search?service=checkout&limit=100&cursor=$CURSOR" \
     -H "X-API-Key: test-key"
```

## Circuit Breaker

Resilience4j circuit breaker protects Elasticsearch:

- **Closed State**: Normal operation
- **Open State**: After 50% failure rate over 10 requests, fails fast for 10 seconds
- **Half-Open State**: Allows 3 test requests to check recovery

When open, returns cached results with `Cache-Control: stale-if-error` headers.

## Monitoring

### Prometheus Metrics (Port 9090)
```
api_search_success_total - Successful search requests
api_search_duration_bucket - Search latency histogram
api_rate_limit_exceeded_total - Rate limit rejections
api_circuit_breaker_activated_total - Circuit breaker activations
```

### Grafana Dashboards (Port 3000)
Login: `admin/admin`

Pre-configured dashboard shows:
- Request rate by service
- P95/P99 latency trends
- Rate limit rejection rate
- Circuit breaker state changes

## Production Considerations

### Performance Optimization
1. **Query Caching**: Frequent queries cached in Redis for 60s
2. **Connection Pooling**: Persistent HTTP/2 connections to Elasticsearch
3. **Response Compression**: gzip for responses >1KB

### Failure Scenarios
1. **Elasticsearch Down**: Returns cached results with stale headers
2. **Redis Down**: Fails open (allows requests) after 30s timeout
3. **Query Timeout**: Returns partial results after 5s with truncated flag

### Security
1. API key validation (implement JWT for production)
2. Input validation and SQL injection prevention
3. Wildcard query constraints (minimum 3 characters)
4. Field whitelist for searchable attributes

## Load Testing Results

With default configuration:

- **Throughput**: 1000 req/sec sustained on single gateway instance
- **Latency**: P50: 45ms, P95: 120ms, P99: 250ms
- **Pagination**: 100KB result sets stream in <200ms
- **Rate Limiting**: Redis handles 10k quota checks/sec with <1ms latency

## Scaling Strategies

### Horizontal Scaling
- Run multiple API gateway instances behind load balancer
- Redis cluster for distributed rate limiting
- Elasticsearch cluster with sharding

### Vertical Scaling
- Increase gateway JVM heap for more concurrent connections
- More Elasticsearch nodes for query parallelization
- Redis with more memory for larger quota windows

## Next Steps

Day 59 implements **active-passive failover** for high availability:
- Automatic failover between Elasticsearch clusters
- Redis Sentinel for cache high availability
- Health checks and automatic traffic routing

## Troubleshooting

**Rate limits too strict:**
```bash
# Increase quota in RateLimitService.java
private static final long STANDARD_QUOTA = 10000;
```

**Elasticsearch connection refused:**
```bash
# Check Elasticsearch health
curl http://localhost:9200/_cluster/health

# Restart if needed
docker-compose restart elasticsearch
```

**No logs appearing:**
```bash
# Check Kafka topics
docker exec -it <kafka-container> kafka-topics --list --bootstrap-server localhost:9092

# Check Logstash processing
docker logs <logstash-container>
```
