# Distributed Log Collector System

A production-ready distributed log processing system built with Spring Boot, demonstrating real-world scalability patterns used by companies like Netflix and Uber.

## 🏗️ System Architecture

This system implements a complete log processing pipeline with the following components:

- **Log Generator**: Produces configurable log events simulating real application traffic
- **Log Collector**: Watches file systems and streams log events to Kafka with exactly-once semantics
- **API Gateway**: Provides unified access to system statistics and health checks
- **Message Streaming**: Apache Kafka for reliable event delivery
- **Caching Layer**: Redis for offset management and deduplication
- **Persistence**: PostgreSQL for long-term log storage
- **Monitoring**: Prometheus + Grafana for comprehensive observability

## 🚀 Quick Start

### 1. Setup Infrastructure
```bash
./setup.sh
```

This will start all required infrastructure services using Docker Compose.

### 2. Build and Run Services
```bash
# Build all modules
mvn clean compile

# Start services (in separate terminals)
mvn spring-boot:run -pl log-generator
mvn spring-boot:run -pl log-collector  
mvn spring-boot:run -pl api-gateway
```

### 3. Verify System
```bash
# Check system health
curl http://localhost:8080/api/health

# View system statistics
curl http://localhost:8080/api/system/stats

# Run integration tests
./integration-tests/system-test.sh
```

## 📊 Monitoring & Observability

### Access Points
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
- **Prometheus Metrics**: http://localhost:9090
- **System Statistics**: http://localhost:8080/api/system/stats

### Key Metrics
- Events generated per second
- Events processed per second
- Kafka delivery success/failure rates
- Circuit breaker status
- Memory and CPU utilization
- Response time percentiles

## 🧪 Testing

### Integration Tests
```bash
./integration-tests/system-test.sh
```

### Load Testing
```bash
./load-test.sh
```

This will increase log generation rate and monitor system performance under load.

## 📁 Project Structure

```
distributed-log-collector/
├── api-gateway/           # API Gateway service
├── log-collector/         # Core log collection service  
├── log-generator/         # Log event generator
├── monitoring/           # Prometheus & Grafana configs
├── integration-tests/    # End-to-end tests
├── docker-compose.yml    # Infrastructure setup
├── setup.sh             # Environment setup script
└── load-test.sh         # Load testing script
```

## 🔧 Configuration

### Log Collection Settings
Edit `log-collector/src/main/resources/application.yml`:

```yaml
log-collector:
  watch-directories: /tmp/logs,/var/log/apps
  batch-size: 100
  flush-interval: 5s
```

### Log Generation Settings
Edit `log-generator/src/main/resources/application.yml`:

```yaml
log-generator:
  output-directory: /tmp/logs
  events-per-second: 50
```

## 🔄 Distributed System Patterns

This implementation demonstrates several key patterns:

### 1. File System Watching
- Java NIO.2 WatchService for real-time file monitoring
- Offset-based reading for exactly-once processing
- Graceful handling of file rotation

### 2. Circuit Breaker Pattern
- Resilience4j for automatic failure detection
- Configurable failure thresholds and timeouts
- Fallback mechanisms for service degradation

### 3. Event Streaming
- Kafka for reliable message delivery
- Batch processing for improved throughput
- Dead letter queues for failed messages

### 4. Distributed State Management
- Redis for offset tracking across restarts
- Deduplication using content hashing
- Eventual consistency with local fallbacks

### 5. Observability
- Distributed tracing with Micrometer
- Custom metrics for business logic
- Health checks and readiness probes

## 🎯 Production Considerations

### Scaling Strategies
- **Horizontal**: Multiple collector instances with Kafka partitioning
- **Vertical**: Tune JVM settings and thread pool sizes
- **Storage**: Partition log data by service and time

### Performance Tuning
- Batch size optimization based on throughput requirements
- Memory management for high-volume log processing
- Network buffer tuning for Kafka producers

### Operational Excellence
- Log rotation handling and cleanup
- Monitoring and alerting on key metrics
- Backup and disaster recovery procedures

## 🏢 Enterprise Scale Connections

This architecture scales to handle:
- **Netflix**: 8 trillion events/day with similar patterns
- **Uber**: 100TB+ daily log processing
- **Amazon**: CloudWatch Logs architecture patterns

Key scaling factors:
- Kafka partitioning strategy
- Redis cluster configuration  
- Database sharding approaches
- Container orchestration with Kubernetes

## 🐛 Troubleshooting

### Common Issues
1. **Kafka connection failures**: Check if Kafka is running and accessible
2. **Redis connection errors**: Verify Redis service and network connectivity
3. **File permission issues**: Ensure log directories are writable
4. **High memory usage**: Tune batch sizes and JVM heap settings

### Debug Commands
```bash
# Check service logs
docker compose logs kafka
docker compose logs redis

# Monitor Kafka topics
docker exec kafka kafka-console-consumer --topic log-events --bootstrap-server localhost:9092

# Check Redis keys
docker exec redis redis-cli KEYS "*"
```

## 📚 Next Steps

After completing this lesson, you'll be ready to:
- Implement log parsing and structured data extraction
- Add stream processing with real-time analytics  
- Build alerting systems based on log patterns
- Deploy to Kubernetes with auto-scaling
- Integrate with enterprise logging platforms

## 🤝 Contributing

This is a learning project. Feel free to experiment with:
- Additional log formats and parsers
- Different message queue technologies
- Alternative storage backends
- Enhanced monitoring capabilities

---

Built as part of the 254-Day System Design Course - Day 3: Log Collection
