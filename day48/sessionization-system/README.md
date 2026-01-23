# Day 48: Sessionization System for User Activity Tracking

## System Overview

Production-grade sessionization system built with Kafka Streams that automatically groups user events into behavioral sessions. Processes 500+ events per second with sub-5ms query latency through multi-tier caching (Redis + RocksDB state stores).

### Architecture Highlights

- **Session Window Processing**: 30-minute inactivity gap windows with grace period handling
- **Real-Time State Queries**: Interactive queries against Kafka Streams state stores
- **Multi-Tier Caching**: Redis (active sessions) → RocksDB (Kafka Streams) → PostgreSQL (historical)
- **Behavioral Analytics**: Session metrics, conversion tracking, and pattern recognition

## Quick Start

### Prerequisites
- Docker & Docker Compose
- 8GB RAM minimum
- Ports: 8080-8083, 9092, 6379, 5432, 9090, 3000

### Deploy System (2 minutes)

```bash
# Start all services
docker-compose up -d

# Wait for services to initialize (30 seconds)
sleep 30

# Verify health
curl http://localhost:8080/health
```

### Run Integration Tests

```bash
cd integration-tests
./test-sessionization.sh
```

### Run Load Tests

```bash
./load-test.sh
```

## System Components

### 1. Event Producer (Port 8081)
Generates realistic user activity events simulating 50 concurrent users with conversion funnel behavior.

**Event Types**: PAGE_VIEW, CLICK, ADD_TO_CART, SEARCH, PURCHASE

**Generation Rate**: 500 events/sec (10 events/sec per user)

### 2. Session Processor (Port 8082)
Kafka Streams application performing sessionization with 30-minute inactivity windows.

**Processing**: Session window aggregation, late event handling, Redis caching

**State Store**: RocksDB-backed with changelog topic for fault recovery

### 3. Session Analytics (Port 8083)
PostgreSQL persistence layer consuming completed sessions for historical analytics.

**Storage**: JSONB columns for flexible behavioral data queries

**Indexes**: Optimized for user lookups and conversion analysis

### 4. API Gateway (Port 8080)
Unified REST API exposing session data from Redis cache and analytics service.

## API Endpoints

### Active Session Lookup
```bash
# Get current active session (cached in Redis)
curl http://localhost:8080/api/sessions/active/user-1 | jq
```

### Session History
```bash
# Get all historical sessions for a user
curl http://localhost:8080/api/sessions/history/user-1 | jq
```

### Analytics Stats
```bash
# Get aggregated stats (last 24 hours)
curl http://localhost:8080/api/analytics/stats?hours=24 | jq

# Sample response:
# {
#   "totalSessions": 1250,
#   "averageDuration": 180.5,
#   "averageEventCount": 12.3,
#   "conversionCount": 85,
#   "conversionRate": 0.068
# }
```

### Converted Sessions
```bash
# Get sessions with purchases
curl http://localhost:8080/api/sessions/converted?size=10 | jq
```

## Monitoring

### Prometheus Metrics (Port 9090)
```bash
# View available metrics
open http://localhost:9090

# Key metrics:
# - kafka_producer_record_send_total: Event generation rate
# - kafka_streams_state_store_records: Active session count
# - session_duration_seconds: Session length distribution
```

### Grafana Dashboard (Port 3000)
```bash
# Access Grafana
open http://localhost:3000
# Login: admin/admin

# Pre-configured dashboard shows:
# - Events per second
# - Active sessions over time
# - Session duration percentiles (p50, p95, p99)
# - Conversion rate trends
```

### Direct Database Queries
```bash
# Check persisted sessions
docker exec -it $(docker ps -qf "name=postgres") \
    psql -U postgres -d sessiondb

# Example queries:
SELECT COUNT(*) FROM user_sessions;
SELECT user_id, COUNT(*) as session_count 
FROM user_sessions 
GROUP BY user_id 
ORDER BY session_count DESC 
LIMIT 10;

# Check conversion stats
SELECT 
    COUNT(*) as total,
    SUM(CASE WHEN has_conversion THEN 1 ELSE 0 END) as converted,
    ROUND(100.0 * SUM(CASE WHEN has_conversion THEN 1 ELSE 0 END) / COUNT(*), 2) as conversion_rate
FROM user_sessions;
```

## Performance Characteristics

### Throughput
- **Event Generation**: 500 events/sec (configurable)
- **Session Processing**: 1000+ events/sec per partition
- **Query Latency**: <5ms (Redis cache), <20ms (interactive queries)

### Resource Usage
- **Memory**: ~4GB total (Kafka 1.5GB, services 2GB, infra 0.5GB)
- **CPU**: 2-4 cores under load
- **Disk**: ~2GB for state stores and databases

### Scalability Patterns
- **Horizontal**: Add Kafka partitions + session-processor replicas
- **State Management**: RocksDB scales to 100GB+ per instance
- **Cache TTL**: 35-minute Redis TTL (5min buffer beyond session gap)

## Distributed Systems Patterns

### Session Window Semantics
```
Event stream: [e1, e2, --25min gap--, e3, e4, --35min gap--, e5]
Result: Session1{e1,e2,e3,e4}, Session2{e5}
```

### Late Event Handling
- **Grace Period**: 5 minutes for out-of-order events
- **Watermarks**: Kafka Streams tracks event time progress
- **Session Merging**: Late events can bridge separate sessions

### Fault Tolerance
- **State Recovery**: Changelog topics enable full state reconstruction
- **Replication**: 3x replication for state stores
- **Failover**: Sub-30s recovery time with standby replicas

## Troubleshooting

### No Sessions Appearing
```bash
# Check event producer is generating
docker logs $(docker ps -qf "name=event-producer") --tail 50

# Verify Kafka has events
docker exec -it $(docker ps -qf "name=kafka") \
    kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic user-events --max-messages 5
```

### High Memory Usage
```bash
# Check RocksDB state store size
docker exec -it $(docker ps -qf "name=session-processor") \
    du -sh /tmp/kafka-streams/

# Solution: Increase num.partitions or reduce grace period
```

### Slow Queries
```bash
# Check Redis cache hit rate
docker exec -it $(docker ps -qf "name=redis") \
    redis-cli INFO stats | grep keyspace

# Check PostgreSQL query performance
docker exec -it $(docker ps -qf "name=postgres") \
    psql -U postgres -d sessiondb -c "SELECT * FROM pg_stat_statements;"
```

## Real-World Applications

### E-Commerce Session Analytics
- Track user journey from landing to purchase
- Identify drop-off points in conversion funnel
- Personalize recommendations based on session behavior

### Content Platform Engagement
- Measure binge-watching patterns
- Compute session-based metrics (watch time, completion rate)
- Trigger notifications for abandoned sessions

### Mobile App User Behavior
- Handle offline-to-online session continuity
- Track feature usage within sessions
- Detect anomalous behavior patterns

## Scaling to Production

### Netflix Scale (250M Users)
- **Partitioning**: 500+ Kafka partitions with consistent hashing
- **Grace Periods**: Tiered processing (5min, 1hr, 24hr pipelines)
- **Query Layer**: Presto/Druid for historical analytics at scale

### Optimization Strategies
1. **State Store Compression**: Enable RocksDB compression (2-3x size reduction)
2. **Partition Pruning**: Route queries to specific partitions based on user ID
3. **Caching Tiers**: L1 (JVM) → L2 (Redis) → L3 (Kafka Streams) → L4 (PostgreSQL)

## Clean Up

```bash
# Stop all services
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v

# Clean up generated files
rm -rf target/ */target/
```

## Next Steps

Tomorrow (Day 49): Implement anomaly detection algorithms to identify unusual session patterns in real-time using z-score analysis and sliding baseline windows.

## Additional Resources

- [Kafka Streams Session Windows](https://kafka.apache.org/documentation/streams/developer-guide/dsl-api.html#session-windows)
- [Session-Based Analytics at Scale](https://netflixtechblog.com/user-and-session-analytics-24b5f2d4c9ac)
- [State Store Interactive Queries](https://docs.confluent.io/platform/current/streams/developer-guide/interactive-queries.html)
