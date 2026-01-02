# Day 35: Topic-Based Routing - Multi-Pipeline Log Processing System

## Overview

This system demonstrates intelligent routing of log events to specialized processing pipelines using content-based routing rules. Logs are evaluated against dynamic routing rules and directed to appropriate Kafka topics, where dedicated consumers process them according to their specific requirements.

## Architecture

### Components

1. **Log Producer** (Port 8080)
   - Generates diverse log events (security, performance, application, system)
   - Sends logs to routing service via REST API
   - Simulates real-world log production patterns

2. **Routing Service** (Port 8081)
   - Evaluates routing rules from Redis
   - Implements content-based routing logic
   - Publishes to multiple Kafka topics (fanout pattern)
   - Provides rule management API

3. **Specialized Consumers**
   - **Security Consumer** (Port 8082): Processes security events, triggers alerts
   - **Performance Consumer** (Port 8083): Aggregates metrics, calculates statistics
   - **Application Consumer** (Port 8084): Tracks errors, integrates with ticketing
   - **System Consumer** (Port 8085): Archives audit logs, handles default routing

### Routing Rules

Rules are stored in Redis and evaluated in priority order:

```yaml
Priority 1: Security Critical
- Conditions: severity=[ERROR,FATAL] AND source=[auth-service,payment-api]
- Destinations: [logs-security, logs-critical]

Priority 2: Performance Metrics
- Conditions: type=metric
- Destinations: [logs-performance]

Priority 3: Application Errors
- Conditions: severity=[ERROR,FATAL] AND type=application
- Destinations: [logs-application, logs-critical]

Priority 4: System Logs
- Conditions: type=[system,audit]
- Destinations: [logs-system]

Priority 999: Default
- Conditions: (always matches)
- Destinations: [logs-default]
```

### Kafka Topics

| Topic | Partitions | Consumer Group | Purpose |
|-------|-----------|----------------|---------|
| logs-security | 16 | security-consumer-group | Real-time security events |
| logs-critical | 8 | critical-consumer-group | Immediate escalation |
| logs-performance | 12 | performance-consumer-group | Metrics aggregation |
| logs-application | 8 | application-consumer-group | Error tracking |
| logs-system | 4 | system-consumer-group | Audit & archival |
| logs-default | 4 | system-consumer-group | Catch-all routing |

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Setup

```bash
# 1. Generate the system
chmod +x setup.sh
./setup.sh

# 2. Build all services
mvn clean install

# 3. Start routing service
cd routing-service
mvn spring-boot:run &

# 4. Start all consumers (in separate terminals)
cd security-consumer && mvn spring-boot:run &
cd performance-consumer && mvn spring-boot:run &
cd application-consumer && mvn spring-boot:run &
cd system-consumer && mvn spring-boot:run &

# 5. Start log producer
cd log-producer
mvn spring-boot:run &
```

### Testing

```bash
# Integration tests
./integration-tests/test-routing.sh

# Load testing
./load-test.sh

# Manual testing
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "severity": "ERROR",
    "source": "payment-api",
    "type": "security",
    "message": "Unauthorized access attempt"
  }'

# View routing rules
curl http://localhost:8081/api/rules
```

## Monitoring

### Prometheus Metrics

- `logs_received_total`: Counter of logs received by routing service
- `logs_routed_total`: Counter of logs routed to topics
- `routing_latency_seconds`: Histogram of routing decision latency
- `security_events_processed_total`: Security events handled
- `performance_metrics_processed_total`: Performance metrics aggregated
- `application_logs_processed_total`: Application logs tracked
- `system_logs_processed_total`: System logs archived

### Access Points

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Log Producer**: http://localhost:8080/actuator/health
- **Routing Service**: http://localhost:8081/actuator/health
- **Security Consumer**: http://localhost:9082/actuator/health
- **Performance Consumer**: http://localhost:9083/actuator/health
- **Application Consumer**: http://localhost:9084/actuator/health
- **System Consumer**: http://localhost:9085/actuator/health

## Performance Characteristics

### Baseline Performance

- **Routing throughput**: 10-15K events/sec per instance
- **Routing latency**: 2-3ms p50, 5-8ms p99
- **Fanout overhead**: ~100μs per additional destination
- **Rule evaluation**: <1ms for 10 rules with regex patterns

### Scalability

- **Horizontal scaling**: Deploy multiple routing service instances behind load balancer
- **Consumer scaling**: Add consumers up to partition count
- **Topic partitioning**: 
  - Security: 16 partitions (high priority, immediate processing)
  - Performance: 12 partitions (batch aggregation)
  - Application: 8 partitions (error tracking)
  - System: 4 partitions (archival can tolerate latency)

### Load Test Results

Expected throughput with single instances:
- 1000 requests: ~100-200ms total (50-100 req/sec)
- 10,000 requests: ~1-2 seconds total (500-1000 req/sec)
- With proper tuning: 10,000-50,000 events/sec

## Key Design Patterns

### 1. Content-Based Routing
Inspect message attributes and route to appropriate destinations based on rules

### 2. Dynamic Rule Management
Store routing rules in Redis, update without deployment

### 3. Multi-Destination Fanout
Single log can route to multiple topics for parallel processing

### 4. Priority-Based Topic Allocation
Different topics with different partition counts and consumer scaling strategies

### 5. Default Routing Fallback
Catch-all topic prevents log loss when rules don't match

## Failure Scenarios

### Rule Compilation Failure
- System falls back to default routing
- Alerts operators to fix rule syntax
- Uses last-known-good rule cache

### Kafka Topic Unavailable
- Logs queued in Redis with TTL
- Retry with exponential backoff
- Circuit breaker prevents cascading failures

### Transaction Timeout
- Reduce fanout destinations
- Implement async fallback for non-critical topics
- Monitor transaction abort rate

### Consumer Lag
- Auto-scaling based on lag thresholds
- Priority-based processing order
- Adaptive throttling for low-priority logs

## Production Considerations

### Capacity Planning

For 100K events/sec:
- 8-10 routing service instances
- 8 security consumers (16 partitions)
- 6 performance consumers (12 partitions)
- 4 application consumers (8 partitions)
- 2 system consumers (4 partitions)

### Monitoring Alerts

- Routing latency p99 > 10ms (bottleneck forming)
- Default topic rate > 5% (routing failures)
- Consumer lag > 1000 messages (scaling needed)
- Transaction abort rate > 1% (downstream issues)

### Operational Runbook

1. **High routing latency**: Scale routing service horizontally
2. **Consumer lag spike**: Add consumer instances
3. **Rule match failures**: Review rule syntax and logs
4. **Kafka broker issues**: Check broker health, disk space

## Next Steps

Day 36 will introduce dead letter queues to handle logs that fail processing despite retries, completing the fault-tolerance architecture.

## References

- Spring Kafka: https://spring.io/projects/spring-kafka
- Apache Kafka: https://kafka.apache.org/documentation/
- Content-Based Routing: Enterprise Integration Patterns
- Routing Slip Pattern: https://www.enterpriseintegrationpatterns.com/
