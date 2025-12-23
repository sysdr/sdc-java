# Day 31: RabbitMQ-Based Distributed Log Processing System

## System Architecture

This production-ready system demonstrates distributed log processing using RabbitMQ message queues with topic-based routing, dead letter queues, and comprehensive monitoring.

### Components

1. **API Gateway** (Port 8080)
   - Entry point for all log submissions
   - Circuit breaker pattern with Resilience4j
   - Rate limiting (100 requests/sec)
   - Routes to Log Producer

2. **Log Producer** (Port 8081)
   - Publishes logs to RabbitMQ topic exchange
   - Implements publisher confirms
   - Retry logic with exponential backoff
   - Generates routing keys based on severity/category

3. **Log Consumer** (Port 8082)
   - Consumes from three queues (critical, processing, monitoring)
   - Manual acknowledgments for guaranteed delivery
   - Idempotency checks using message IDs
   - Persists to PostgreSQL, caches in Redis

4. **RabbitMQ** (Ports 5672, 15672)
   - Topic exchange with wildcard routing
   - Durable queues with TTL
   - Dead letter exchange for failed messages
   - Management UI at http://localhost:15672

5. **PostgreSQL** (Port 5432)
   - Persistent storage for processed logs
   - Indexed by severity, timestamp, category

6. **Redis** (Port 6379)
   - Caching layer for recent logs
   - Session management

7. **Monitoring Stack**
   - Prometheus: Metrics collection (Port 9090)
   - Grafana: Visualization dashboards (Port 3000)

## Message Flow

```
Client → API Gateway → Log Producer → RabbitMQ Topic Exchange
                                           ↓
                              ┌─────────────┼─────────────┐
                              ↓             ↓             ↓
                      logs-critical  logs-processing  logs-monitoring
                              ↓             ↓             ↓
                         Log Consumer (3 listeners)
                              ↓
                      PostgreSQL + Redis
```

## Routing Keys

- `logs.error.*` → Critical queue (high priority)
- `logs.#` → Processing queue (all logs)
- `*.*.auth` → Monitoring queue (authentication logs)

## Quick Start

### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- 8GB RAM minimum
- curl or Postman for testing

### Setup

```bash
# Make setup script executable
chmod +x setup.sh

# Run setup
./setup.sh
```

The setup script will:
1. Start infrastructure (RabbitMQ, PostgreSQL, Redis, Prometheus, Grafana)
2. Wait for services to be healthy
3. Build and deploy application services
4. Verify all services are running

### Verify Deployment

```bash
# Check all services
docker-compose ps

# Test API Gateway
curl http://localhost:8080/api/v1/health

# Check RabbitMQ Management
open http://localhost:15672  # admin/admin123

# Check Grafana
open http://localhost:3000   # admin/admin
```

## Usage Examples

### Publish Single Log

```bash
curl -X POST http://localhost:8080/api/v1/logs \
  -H "Content-Type: application/json" \
  -d '{
    "severity": "error",
    "category": "auth",
    "message": "Failed login attempt from IP 192.168.1.100",
    "source": "auth-service"
  }'
```

### Publish Batch Logs

```bash
curl -X POST http://localhost:8080/api/v1/logs/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "severity": "info",
      "category": "payment",
      "message": "Payment processed successfully",
      "source": "payment-service"
    },
    {
      "severity": "warn",
      "category": "inventory",
      "message": "Low stock alert for product SKU-12345",
      "source": "inventory-service"
    }
  ]'
```

## Testing

### Integration Tests

```bash
./integration-tests/test-flow.sh
```

Tests cover:
- Single log publish
- Batch log publish
- RabbitMQ queue verification
- Consumer processing

### Load Testing

```bash
./load-test.sh
```

Generates:
- 10,000 total requests
- 50 concurrent connections
- ~200 requests/second throughput

Monitor results in:
- Grafana: http://localhost:3000
- RabbitMQ Management: http://localhost:15672

## Monitoring

### Key Metrics

**RabbitMQ Management UI** (http://localhost:15672)
- Queue depths and message rates
- Consumer utilization
- Connection status

**Prometheus** (http://localhost:9090)
- Query: `rabbitmq_messages_published`
- Query: `rabbitmq_messages_processed`
- Query: `rabbitmq_processing_duration_seconds`

**Grafana** (http://localhost:3000)
- Pre-configured Prometheus datasource
- Create dashboards for:
  - Message throughput
  - Queue depths over time
  - Processing latency (p50, p95, p99)
  - Error rates

### Application Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f log-consumer

# Follow with grep
docker-compose logs -f | grep ERROR
```

## Performance Characteristics

### Baseline Performance (4-core, 8GB RAM)
- **Throughput**: 20,000 messages/second
- **Latency (p99)**: 50ms
- **Consumer Concurrency**: 3-10 threads per queue
- **Prefetch Count**: 10 messages per consumer

### Scaling Strategies

1. **Horizontal Consumer Scaling**
   ```bash
   docker-compose up -d --scale log-consumer=3
   ```

2. **Increase Prefetch Count**
   - Modify `application.yml`: `prefetch: 20`
   - Trade-off: Higher throughput, less balanced load

3. **Add Queue Replicas**
   - Configure mirrored queues for critical logs
   - 40-50% throughput reduction

## Troubleshooting

### Services Not Starting

```bash
# Check service logs
docker-compose logs [service-name]

# Restart specific service
docker-compose restart [service-name]

# Full restart
docker-compose down && ./setup.sh
```

### Messages Not Being Consumed

1. Check RabbitMQ Management UI for queue depth
2. Verify consumer connections: http://localhost:15672/#/connections
3. Check consumer logs: `docker-compose logs log-consumer`
4. Verify PostgreSQL connectivity: `docker-compose logs postgres`

### High Memory Usage

- RabbitMQ pages messages to disk at 40% RAM
- Increase VM memory or reduce queue depth
- Enable lazy queues for disk-first storage

### Circuit Breaker Open

- Check log producer health: `curl http://localhost:8081/api/v1/logs/health`
- View circuit breaker status: `curl http://localhost:8080/actuator/circuitbreakers`
- Wait 10 seconds for automatic recovery

## Architecture Decisions

### Why RabbitMQ Over Kafka?

- **Routing Flexibility**: Topic exchanges with wildcard patterns
- **Latency**: Sub-10ms vs. Kafka's 10-50ms
- **Use Case**: Complex routing, priority queues, request-reply
- **Trade-off**: Lower throughput than Kafka (20K vs. 100K+ msgs/sec)

### Manual vs. Auto Acknowledgment

- **Manual**: Guarantees at-least-once delivery
- **Performance Impact**: 30-40% throughput reduction
- **Decision**: Use manual for critical logs, auto for debug logs

### Dead Letter Queue Strategy

- **3 Retry Attempts**: Exponential backoff (1s, 2s, 4s)
- **7-Day Retention**: DLQ messages expire after 1 week
- **Manual Investigation**: DLQ requires human review

## Production Considerations

### High Availability

1. **RabbitMQ Clustering**
   - Deploy 3-node cluster with quorum queues
   - Configure `pause_minority` partition handling

2. **Database Replication**
   - PostgreSQL streaming replication
   - Read replicas for query workloads

3. **Redis Sentinel**
   - Master-slave replication
   - Automatic failover

### Security

- [ ] Enable TLS for RabbitMQ connections
- [ ] Use secrets management (Vault, AWS Secrets Manager)
- [ ] Implement authentication/authorization
- [ ] Network segmentation with VPC

### Monitoring Alerts

Configure alerts for:
- Queue depth > 10,000 messages
- Consumer lag > 30 seconds
- Error rate > 1%
- Circuit breaker open state
- Memory usage > 80%

## Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (data loss!)
docker-compose down -v

# Remove images
docker system prune -a
```

## Next Steps (Day 32)

Tomorrow we'll build the producer service enhancements:
- Connection pooling optimization
- Batch processing for higher throughput
- Advanced retry strategies
- Message compression

## Resources

- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/reference/)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Prometheus Queries](https://prometheus.io/docs/prometheus/latest/querying/basics/)

## License

MIT License - Educational purposes
