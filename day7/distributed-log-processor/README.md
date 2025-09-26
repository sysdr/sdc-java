# Distributed Log Processing System

A production-ready distributed log processing system built with Spring Boot, Apache Kafka, PostgreSQL, and Redis. This system demonstrates key distributed system patterns including event sourcing, CQRS, circuit breakers, and cache-aside patterns.

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Log Producer  │    │  Apache Kafka   │    │  Log Consumer   │
│   (Port 8081)   │───▶│   (Port 9092)   │───▶│   (Port 8082)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │◄──▶│     Redis       │    │   PostgreSQL    │
│   (Port 8080)   │    │   (Port 6379)   │    │   (Port 5432)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       ▲
                                                       │
                                               ┌─────────────────┐
                                               │  Log Consumer   │
                                               └─────────────────┘
```

### Key Components

- **Log Producer**: REST API for log ingestion with rate limiting and circuit breakers
- **Log Consumer**: Kafka consumer that persists logs to PostgreSQL with exactly-once semantics
- **API Gateway**: Query service with Redis caching and advanced search capabilities
- **Apache Kafka**: Distributed streaming platform for reliable message delivery
- **PostgreSQL**: Primary data store with optimized indexing for time-series queries
- **Redis**: High-performance cache for frequently accessed data
- **Monitoring Stack**: Prometheus + Grafana for observability

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17+
- Maven 3.8+
- At least 4GB RAM available

### 1. Start Infrastructure
```bash
# Start all infrastructure services
./setup.sh

# Verify services are healthy
docker-compose ps
```

### 2. Build and Start Applications
```bash
# Build all services
mvn clean package -DskipTests

# Start Log Producer
cd log-producer && mvn spring-boot:run &

# Start Log Consumer  
cd log-consumer && mvn spring-boot:run &

# Start API Gateway
cd api-gateway && mvn spring-boot:run &
```

### 3. Verify System Health
```bash
# Run integration tests
./integration-tests/run-tests.sh

# Check service health
curl http://localhost:8081/producer/api/logs/health
curl http://localhost:8082/actuator/health
curl http://localhost:8080/gateway/api/query/health
```

## 📊 Monitoring and Observability

Access the monitoring dashboards:

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Application Metrics**: 
  - Producer: http://localhost:8081/producer/actuator/prometheus
  - Consumer: http://localhost:8082/actuator/prometheus
  - Gateway: http://localhost:8080/gateway/actuator/prometheus

### Key Metrics to Monitor

- **Throughput**: `log_ingestion_total`, `kafka_messages_consumed_total`
- **Latency**: `log_ingestion_duration`, `log_processing_duration`
- **Errors**: `kafka_consumption_errors_total`, Circuit breaker states
- **Resources**: JVM heap usage, Database connection pool, Redis memory

## 🔧 API Usage

### Ingesting Logs

#### Single Log Event
```bash
curl -X POST http://localhost:8081/producer/api/logs/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR",
    "message": "Database connection failed",
    "source": "user-service",
    "metadata": {
      "userId": "12345",
      "operation": "login",
      "duration": 5000
    }
  }'
```

#### Batch Log Events
```bash
curl -X POST http://localhost:8081/producer/api/logs/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "level": "INFO",
      "message": "User logged in",
      "source": "auth-service"
    },
    {
      "level": "WARN", 
      "message": "Rate limit approaching",
      "source": "api-gateway"
    }
  ]'
```

### Querying Logs

#### Time Range Query
```bash
curl "http://localhost:8080/gateway/api/query/logs?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00"
```

#### Filter by Level and Source
```bash
curl "http://localhost:8080/gateway/api/query/logs?level=ERROR&source=user-service&page=0&size=50"
```

#### Full-Text Search
```bash
curl "http://localhost:8080/gateway/api/query/search?query=database&startTime=2024-01-01T00:00:00"
```

#### Get Statistics
```bash
curl "http://localhost:8080/gateway/api/query/stats?hoursBack=24"
```

## 🧪 Testing

### Load Testing
```bash
# Generate sustained load
./load-test.sh

# Monitor performance during load test
curl http://localhost:8080/gateway/actuator/metrics/http.server.requests
```

### Integration Testing
```bash
# Full end-to-end test suite
./integration-tests/run-tests.sh
```

## ⚡ Performance Tuning

### Producer Optimization
```yaml
# Increase batch size for higher throughput
spring.kafka.producer.batch-size: 32768
spring.kafka.producer.linger-ms: 10

# Tune for latency vs throughput
spring.kafka.producer.compression-type: lz4
```

### Consumer Optimization
```yaml
# Increase parallelism
spring.kafka.listener.concurrency: 6

# Batch processing
spring.kafka.consumer.max-poll-records: 500
```

### Database Optimization
```yaml
# Connection pooling
spring.datasource.hikari.maximum-pool-size: 50
spring.datasource.hikari.minimum-idle: 10

# JPA batching
spring.jpa.properties.hibernate.jdbc.batch_size: 50
```

## 🛡️ Production Considerations

### Security
- Implement authentication/authorization
- Enable SSL/TLS for all communications
- Use Kafka SASL/SCRAM for authentication
- Secure Redis with AUTH and SSL

### Scalability
- **Horizontal scaling**: Add more producer/consumer instances
- **Kafka partitioning**: Increase partitions for higher parallelism
- **Database sharding**: Partition logs by time ranges
- **Read replicas**: Separate read/write database instances

### Monitoring & Alerting
- Set up alerts for circuit breaker trips
- Monitor Kafka consumer lag
- Alert on database connection pool exhaustion
- Track error rates and response times

### Backup & Recovery
- Configure PostgreSQL continuous archiving
- Implement Kafka topic replication
- Regular database backups with point-in-time recovery

## 🔧 Configuration

### Environment Variables
```bash
# Database
POSTGRES_URL=jdbc:postgresql://localhost:5432/logprocessor
POSTGRES_USERNAME=loguser
POSTGRES_PASSWORD=logpass

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC=log-events

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Monitoring
PROMETHEUS_ENABLED=true
GRAFANA_ADMIN_PASSWORD=secure_password
```

### JVM Tuning
```bash
# Production JVM settings
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## 🐛 Troubleshooting

### Common Issues

#### Kafka Connection Issues
```bash
# Check Kafka broker status
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list

# Verify topic creation
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic log-events
```

#### Database Connection Problems
```bash
# Check PostgreSQL logs
docker logs postgres

# Verify database connectivity
psql -h localhost -p 5432 -U loguser -d logprocessor -c "SELECT COUNT(*) FROM log_events;"
```

#### Memory Issues
```bash
# Monitor JVM memory usage
curl http://localhost:8081/producer/actuator/metrics/jvm.memory.used

# Check Docker container resources
docker stats
```

### Performance Debugging
```bash
# Check consumer lag
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group log-consumer-group

# Monitor database performance
docker exec -it postgres psql -U loguser -d logprocessor -c "
SELECT query, calls, mean_time, total_time 
FROM pg_stat_statements 
ORDER BY total_time DESC LIMIT 10;"
```

## 📈 Scaling to Production

This system is designed to scale horizontally. For production deployment:

1. **Deploy to Kubernetes** with proper resource limits and auto-scaling
2. **Use managed services**: Amazon MSK (Kafka), RDS (PostgreSQL), ElastiCache (Redis)
3. **Implement proper monitoring**: DataDog, New Relic, or Elastic APM
4. **Set up CI/CD pipelines** with automated testing and deployment
5. **Configure log aggregation** with ELK Stack or Splunk

The patterns implemented here are the same ones used by:
- **Netflix**: For real-time event processing and analytics
- **Uber**: For ride tracking and driver dispatch systems  
- **Amazon**: For CloudWatch Logs and application monitoring

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
