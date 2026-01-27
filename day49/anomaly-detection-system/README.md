# Day 49: Anomaly Detection System

A production-grade distributed system for detecting anomalies in log streams using statistical methods, time-series analysis, and clustering algorithms.

## Architecture Overview

The system consists of three main services:

1. **Log Producer** (Port 8081)
   - Generates synthetic log events with controllable anomaly injection
   - Produces 10 events/second with periodic anomaly injection
   - Publishes to Kafka `log-events` topic

2. **Anomaly Detector** (Port 8082)
   - Consumes log events from Kafka
   - Applies multiple detection algorithms:
     - Statistical Z-score detection
     - Time-series forecasting with exponential smoothing
     - Multi-dimensional clustering
   - Stores results in Redis (cache) and PostgreSQL (persistence)
   - Publishes detected anomalies to `detected-anomalies` topic

3. **API Gateway** (Port 8080)
   - REST API for querying detected anomalies
   - Multi-tier caching (Redis → PostgreSQL)
   - Endpoints for filtering by service, time range, confidence

## Infrastructure Components

- **Kafka**: Event streaming platform for log events
- **Redis**: High-speed cache for recent anomalies
- **PostgreSQL**: Persistent storage for historical analysis
- **Prometheus**: Metrics collection from all services
- **Grafana**: Visualization dashboards (admin/admin)

## Quick Start

### Prerequisites

- Docker and Docker Compose
- 8GB RAM minimum
- Ports 8080-8082, 9090, 3000, 5432, 6379, 2181, 9092 available

### Setup and Run

```bash
# Make setup script executable and run
chmod +x setup.sh
./setup.sh

# Wait for services to stabilize (about 60 seconds)

# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## API Endpoints

### Get Anomaly by Event ID
```bash
curl http://localhost:8080/api/anomalies/{eventId}
```

### Get Anomalies by Service
```bash
curl http://localhost:8080/api/anomalies/service/auth-service
```

### Get Recent Anomalies
```bash
# Last 24 hours with minimum 50% confidence
curl http://localhost:8080/api/anomalies/recent?hours=24&minConfidence=0.5
```

### Get High Confidence Anomalies
```bash
# Anomalies with >70% confidence
curl http://localhost:8080/api/anomalies/high-confidence?minConfidence=0.7
```

### Get Statistics
```bash
curl http://localhost:8080/api/anomalies/stats
```

## Testing

### Integration Tests
```bash
./integration-tests/test-anomaly-detection.sh
```

Tests verify:
- Service health and connectivity
- Anomaly detection functionality
- API Gateway responses
- End-to-end data flow

### Load Testing
```bash
./load-test.sh
```

Generates load on API Gateway endpoints to verify:
- System performance under stress
- Response times at scale
- Error rates and circuit breaker behavior

## Monitoring

### Prometheus Metrics
Access Prometheus UI at http://localhost:9090

Key metrics to monitor:
- `http_server_requests_seconds_sum`: Request latency
- `kafka_consumer_lag`: Event processing lag
- `resilience4j_circuitbreaker_state`: Circuit breaker status

### Grafana Dashboards
Access Grafana at http://localhost:3000 (admin/admin)

Create dashboards to visualize:
- Anomaly detection rate over time
- Confidence score distribution
- Service-specific anomaly patterns
- System resource utilization

## Anomaly Detection Algorithms

### 1. Statistical Z-Score Detection
- Flags values >3 standard deviations from mean
- Maintains sliding window of 100 observations per metric
- Best for: Gaussian-distributed metrics (response times, CPU usage)

### 2. Time-Series Forecasting
- Uses exponential smoothing to predict expected values
- Detects deviations >50% from forecast
- Best for: Metrics with trends and seasonality

### 3. Multi-Dimensional Clustering
- Simplified isolation forest for outlier detection
- Analyzes 5-dimensional feature space
- Best for: Correlated anomalies across multiple metrics

### Confidence Scoring
- Z-score detection: 40% weight
- Time-series detection: 30% weight
- Clustering detection: 30% weight
- Boost for multiple detector agreement

## Production Considerations

### Scalability
- Kafka partitioning enables horizontal scaling
- Each detector instance maintains independent state
- Redis caching reduces database load
- PostgreSQL handles 10,000+ anomalies/second

### Performance Tuning
- Adjust window sizes for latency vs. accuracy trade-off
- Tune detector thresholds based on false positive rates
- Monitor circuit breaker states
- Implement backpressure if detection lags

### Failure Scenarios
- **Kafka unavailable**: Circuit breaker prevents cascading failures
- **Redis cache miss**: Fallback to PostgreSQL
- **Database failure**: Degraded mode with cache-only operation
- **State corruption**: Replay Kafka topics to rebuild state

## Architecture Patterns

### Multi-Layer Detection
Combines complementary algorithms to reduce false positives while maintaining high detection rates.

### Stateful Stream Processing
Uses Kafka Streams with RocksDB for efficient window aggregations and state management.

### CQRS Pattern
Separates write path (anomaly detection) from read path (API queries) for independent scaling.

### Circuit Breaker
Resilience4j provides fault tolerance, preventing cascading failures during downstream service outages.

## Troubleshooting

### Services won't start
```bash
docker-compose logs [service-name]
docker-compose down -v
./setup.sh
```

### No anomalies detected
Check log producer is running:
```bash
docker-compose logs log-producer
```

### High false positive rate
Tune detection thresholds in:
- `StatisticalDetector.java`: Z_SCORE_THRESHOLD
- `TimeSeriesDetector.java`: DEVIATION_THRESHOLD
- `ClusteringDetector.java`: ANOMALY_THRESHOLD

## Next Steps

Extend the system with:
1. Alert generation and routing (Day 50)
2. Machine learning model training pipeline
3. Anomaly correlation across services
4. Custom detector plugins
5. Multi-datacenter deployment

## Performance Benchmarks

Expected throughput on 4-core, 8GB system:
- Log ingestion: 10,000 events/second
- Anomaly detection: 5,000 events/second processed
- API queries: 1,000 requests/second
- Detection latency: <100ms p99

## License

MIT License - See LICENSE file for details
