# Day 46: Time-Based Windowing for Real-Time Log Aggregation

## System Overview

Production-grade distributed log analytics system implementing time-based windowing patterns:

- **Tumbling Windows**: 5-minute non-overlapping aggregations
- **Hopping Windows**: 10-minute overlapping windows (2-minute advance)
- **Session Windows**: Dynamic windows based on 5-minute inactivity gaps
- **Real-time Processing**: 50,000+ events/second with sub-100ms latency
- **Exactly-Once Semantics**: Fault-tolerant state management with RocksDB
- **Late Data Handling**: Grace periods and watermark management

## Architecture

```
Log Producer (8 services)
    ↓ (Kafka: raw-logs topic)
Window Processor (Kafka Streams)
    ├── Tumbling Windows (5min)
    ├── Hopping Windows (10min/2min)
    └── Session Windows (5min gap)
    ↓
RocksDB State Stores + PostgreSQL + Redis
    ↓
API Gateway (Query Interface)
```

## Quick Start

### 1. Start System

```bash
cd day46-windowed-log-analytics
./setup.sh
```

Wait 2-3 minutes for:
- Infrastructure initialization
- Service startup
- Window population

### 2. Access Services

- **API Gateway**: http://localhost:8080
- **Log Producer**: http://localhost:8081
- **Window Processor**: http://localhost:8082
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## API Endpoints

### Get Recent Windows
```bash
curl "http://localhost:8080/api/windows/recent?limit=20"
```

### Query by Service and Time Range
```bash
FROM=$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)
TO=$(date -u +%Y-%m-%dT%H:%M:%SZ)

curl "http://localhost:8080/api/windows/service/api-gateway?windowType=TUMBLING&from=${FROM}&to=${TO}"
```

### Get Aggregated Statistics
```bash
curl "http://localhost:8080/api/windows/stats?from=${FROM}&to=${TO}"
```

## Window Types Explained

### Tumbling Windows (5 minutes)
- Non-overlapping fixed segments
- Example: 10:00-10:05, 10:05-10:10, 10:10-10:15
- Each event belongs to exactly one window
- Best for: Discrete period reporting (metrics dashboards)

### Hopping Windows (10 minutes / 2 minutes)
- Overlapping segments advancing every 2 minutes
- Example: 10:00-10:10, 10:02-10:12, 10:04-10:14
- Each event appears in 5 windows (overlap factor: 10/2)
- Best for: Trend detection, smoothed moving averages

### Session Windows (5 minute gap)
- Dynamic windows merging activity within gap threshold
- Example: Events at 10:00, 10:03, 10:06 → single window [10:00-10:11]
- Window extends 5 minutes after last event
- Best for: User sessions, transaction flows

## Monitoring

### Grafana Dashboard
- URL: http://localhost:3000
- Credentials: admin/admin
- Pre-configured dashboard: "Windowed Log Analytics Dashboard"

### Key Metrics
- Events processed per second
- Window processing errors
- Kafka Streams lag
- JVM memory usage
- State store size

### Prometheus
- URL: http://localhost:9090
- All application metrics exposed at `/actuator/prometheus`

## Available Services

The system generates logs for 8 services:
- api-gateway
- auth-service
- user-service
- order-service
- payment-service
- notification-service
- inventory-service
- analytics-service

## Performance Characteristics

### Throughput
- **Producer**: 400 events/second (50 events/sec × 8 services)
- **Window Processor**: 50,000 events/second capacity
- **API Gateway**: 1,000 queries/second

### Latency
- Event ingestion: 5-10ms
- Window assignment: 1-3ms
- State update: 5-10ms
- Total: sub-100ms event-to-result

## Data Flow

1. **Log Producer** generates events with event timestamps
2. **Kafka** partitions by service name for parallel processing
3. **Timestamp Extractor** uses event time for windowing
4. **Window Processor** assigns events to windows, updates state
5. **State Store** persists aggregations in RocksDB
6. **Window Close** (on watermark + grace period) triggers:
   - Changelog write to Kafka
   - Result persistence to PostgreSQL
   - Cache write to Redis
7. **API Gateway** queries:
   - Redis for recent windows (2-hour TTL)
   - PostgreSQL for historical queries
   - Interactive queries for active windows

## Troubleshooting

### No Windows Appearing
```bash
# Check Kafka Streams state
curl http://localhost:8082/actuator/kafkastreams

# Verify log production
docker-compose logs log-producer | grep "Sent event"

# Check processor logs
docker-compose logs window-processor | grep "Window completed"
```

### High Latency
```bash
# Monitor Kafka lag
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group windowed-log-analytics

# Check RocksDB metrics
curl http://localhost:8082/actuator/metrics/kafka.streams.rocksdb
```

### State Store Errors
```bash
# Reset Kafka Streams state
docker-compose down
docker volume rm $(docker volume ls -q | grep streams_state)
docker-compose up -d
```

## Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (destroys data)
docker-compose down -v
```

## Technology Stack

- **Spring Boot 3.2.0**: Application framework
- **Kafka Streams**: Stream processing engine
- **PostgreSQL**: Persistent storage for window results
- **Redis**: Caching layer for fast queries
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **RocksDB**: Embedded state store for Kafka Streams
