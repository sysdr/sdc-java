# Day 30: Distributed Log Cluster Performance Monitoring

## Overview

Production-grade performance measurement and optimization system for distributed log processing clusters. Implements comprehensive metrics collection, automated bottleneck detection, and intelligent load testing.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Load Generator │────▶│ Performance      │────▶│ Metrics         │
│  (Port 8081)    │     │ Monitor          │     │ Analyzer        │
│                 │     │ (Port 8080)      │     │ (Port 8082)     │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                               │    │
                               │    │
                        ┌──────┘    └───────┐
                        ▼                    ▼
                  ┌──────────┐        ┌─────────────┐
                  │Prometheus│────────▶│   Grafana   │
                  │(Port 9090│        │ (Port 3000) │
                  └──────────┘        └─────────────┘
```

### Components

1. **Performance Monitor Service** (Port 8080)
   - Collects metrics from all cluster nodes
   - Tracks latency percentiles (p50, p95, p99)
   - Monitors resource utilization (CPU, memory, I/O)
   - Exposes Prometheus metrics

2. **Load Generator Service** (Port 8081)
   - Simulates production traffic patterns
   - Implements burst and ramp test scenarios
   - Uses token bucket rate limiting
   - Tracks request success rates and latencies

3. **Metrics Analyzer Service** (Port 8082)
   - Statistical analysis of performance data
   - Automated bottleneck detection
   - Capacity planning projections
   - Generates optimization recommendations

4. **Infrastructure**
   - Kafka: Message streaming (6 partitions)
   - Redis: Caching layer
   - PostgreSQL: Persistent storage
   - Prometheus: Metrics collection
   - Grafana: Visualization

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- 8GB+ RAM

### Installation

```bash
# Generate and run system
chmod +x setup.sh
./setup.sh

# Build services
mvn clean package

# Start Performance Monitor
cd performance-monitor
mvn spring-boot:run

# In another terminal, start Load Generator
cd load-generator
mvn spring-boot:run

# In another terminal, start Metrics Analyzer
cd metrics-analyzer
mvn spring-boot:run
```

### Running Load Tests

```bash
# Execute comprehensive load tests
./load-test.sh

# Or run individual tests via API
curl -X POST http://localhost:8081/api/loadtest/burst \
  -H "Content-Type: application/json" \
  -d '{
    "testName": "high-throughput-test",
    "baselineRatePerSecond": 5000,
    "burstRatePerSecond": 20000,
    "baselineDuration": "PT2M",
    "burstDuration": "PT1M"
  }'
```

## Performance Testing Scenarios

### 1. Burst Load Test

Tests system behavior during traffic spikes:

- **Phase 1**: Baseline (5,000 events/sec for 2 minutes)
- **Phase 2**: Burst (20,000 events/sec for 60 seconds)
- **Phase 3**: Recovery (5,000 events/sec for 2 minutes)

**Expected Results**:
- P95 latency: <100ms during baseline
- P95 latency: <200ms during burst
- No errors or dropped requests
- Graceful degradation if capacity exceeded

### 2. Ramp Load Test

Validates scaling behavior:

- **Phase 1**: Start at 1,000 events/sec
- **Phase 2**: Ramp to 15,000 events/sec over 10 minutes
- **Phase 3**: Maintain peak load for observation

**Expected Results**:
- Linear throughput increase
- Latency remains stable
- Resource utilization scales proportionally

## API Endpoints

### Performance Monitor (Port 8080)

```bash
# Get current metrics for all components
GET http://localhost:8080/api/performance/metrics

# Get latency percentiles
GET http://localhost:8080/api/performance/latency

# Detect bottlenecks
GET http://localhost:8080/api/performance/bottlenecks

# Capture performance baseline
POST http://localhost:8080/api/performance/baseline

# Generate comprehensive report
GET http://localhost:8080/api/performance/report?durationMinutes=60

# Record event (for testing)
POST http://localhost:8080/api/performance/record?component=api-gateway&latencyMs=45
```

### Load Generator (Port 8081)

```bash
# Execute burst test
POST http://localhost:8081/api/loadtest/burst

# Execute ramp test
POST http://localhost:8081/api/loadtest/ramp

# Get test results
GET http://localhost:8081/api/results/latest
```

## Monitoring & Dashboards

### Prometheus Metrics

Access Prometheus at http://localhost:9090

Key metrics:
- `cluster_events_total`: Total events processed
- `cluster_latency`: Latency distribution with percentiles
- `system_cpu_usage`: CPU utilization percentage
- `system_memory_usage`: Memory utilization percentage

### Grafana Dashboards

Access Grafana at http://localhost:3000 (admin/admin)

Pre-configured dashboards:
1. **Cluster Performance Overview**
   - Throughput trends
   - Latency percentiles
   - Resource utilization

2. **Bottleneck Analysis**
   - Component-level metrics
   - Utilization vs throughput
   - Queue depths

3. **Capacity Planning**
   - Growth trends
   - Projected capacity
   - Scaling recommendations

## Performance Optimization

### Bottleneck Detection

The system automatically detects bottlenecks by analyzing:

1. **CPU Bottleneck**: >75% CPU with <80% target throughput
   - **Solution**: Horizontal scaling (add 2-4 nodes)

2. **Memory Bottleneck**: >80% memory usage
   - **Solution**: Increase heap size by 25%, tune GC

3. **Queue Bottleneck**: Queue depth >1000 messages
   - **Solution**: Increase Kafka partitions, scale consumers

4. **I/O Bottleneck**: >80% disk/network utilization
   - **Solution**: Upgrade storage, increase buffers

### Configuration Tuning

Based on performance analysis, tune these parameters:

```yaml
# JVM Tuning
-Xmx4g                    # Heap size
-XX:+UseG1GC              # G1 garbage collector
-XX:MaxGCPauseMillis=200  # GC pause target

# Kafka Configuration
num.partitions=6          # Match consumer count
batch.size=16384          # Optimize throughput

# Redis Configuration
maxmemory=256mb
maxmemory-policy=allkeys-lru

# PostgreSQL Connection Pool
maximum-pool-size=50
minimum-idle=10
```

## Performance Targets

### Local Development

- **Throughput**: 10,000 events/sec sustained
- **Latency**: P95 <50ms, P99 <100ms
- **Availability**: 99.9% (8.76 hours downtime/year)
- **Resource Utilization**: <60% CPU, <70% memory

### Production Scale

Examples from real-world systems:

- **Datadog**: 3.5M metrics/sec, p99 <120ms
- **Meta Scuba**: 3M events/sec, 1-second query latency
- **Netflix**: 1T events/day, <50ms p95 latency

## Troubleshooting

### High Latency

```bash
# Check latency distribution
curl http://localhost:8080/api/performance/latency

# Identify slow components
curl http://localhost:8080/api/performance/metrics

# Look for bottlenecks
curl http://localhost:8080/api/performance/bottlenecks
```

### Low Throughput

```bash
# Check resource utilization
curl http://localhost:8080/api/performance/metrics

# Verify Kafka partition count
docker exec -it $(docker ps -q -f name=kafka) \
  kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic log-events

# Increase partitions if needed
docker exec -it $(docker ps -q -f name=kafka) \
  kafka-topics --bootstrap-server localhost:9092 \
  --alter --topic log-events --partitions 12
```

### Memory Issues

```bash
# Check GC metrics
curl http://localhost:8080/actuator/metrics/jvm.gc.pause

# Analyze heap usage
jmap -heap <PID>

# Generate heap dump
jmap -dump:format=b,file=heap.bin <PID>
```

## Next Steps

Tomorrow (Day 31) we transition to **Module 2: Scalable Log Processing** with RabbitMQ:

- Message queue patterns
- Competing consumers
- Topic-based routing
- Queue-based load leveling

## Learning Objectives

After completing this lesson, you should be able to:

1. ✅ Implement comprehensive performance monitoring
2. ✅ Design realistic load testing scenarios
3. ✅ Detect and diagnose bottlenecks automatically
4. ✅ Generate actionable optimization recommendations
5. ✅ Perform capacity planning for growth
6. ✅ Understand how local patterns scale to global systems

## References

- **Netflix**: Chaos Engineering and Performance Testing
- **Uber**: Surge Pricing with Sub-100ms Latency
- **Amazon**: 1% Conversion Loss per 100ms Delay
- **Datadog**: Processing 3.5M Metrics/Second
- **Meta Scuba**: Real-time Analytics at 3M Events/Sec

## Success Metrics

System is production-ready when:

- [x] All services start within 3 minutes
- [x] Load tests complete without errors
- [x] P95 latency <100ms at 10K events/sec
- [x] Bottleneck detection identifies issues correctly
- [x] Performance reports provide actionable insights
- [x] Grafana dashboards display real-time metrics

---

**Module 1 Complete!** 🎉

You've built a complete distributed log processing system with comprehensive performance monitoring. Module 2 begins tomorrow with message queues and scalable processing patterns.
