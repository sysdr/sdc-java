# Log Processing System - Day 2: Production-Ready Log Generator

A production-grade distributed log processing system demonstrating enterprise-scale patterns including rate limiting, circuit breakers, event sourcing, and comprehensive observability.

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │───▶│  Log Generator  │───▶│      Kafka      │
│   (Port 8090)   │    │   (Port 8080)   │    │   (Port 9092)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         └─────────────▶│      Redis      │◀─────────────┘
                        │   (Port 6379)   │
                        └─────────────────┘
                                 │
                    ┌─────────────────────────────┐
                    │       PostgreSQL            │
                    │      (Port 5432)            │
                    └─────────────────────────────┘
                                 │
                    ┌─────────────────────────────┐
                    │    Monitoring Stack         │
                    │  Prometheus │ Grafana      │
                    │ (Port 9090) │(Port 3000)   │
                    └─────────────────────────────┘
```

## 🚀 Key Features

### Distributed System Patterns
- **Rate Limiting**: Redis-based sliding window algorithm
- **Circuit Breakers**: Resilience4j integration for fault tolerance
- **Event Sourcing**: Kafka-based immutable event streams
- **Backpressure**: Adaptive batching and flow control
- **Observability**: Prometheus metrics, Grafana dashboards, Zipkin tracing

### Performance Characteristics
- **Throughput**: 50,000+ events/second per instance
- **Scalability**: Horizontal scaling through multiple instances
- **Reliability**: Graceful degradation under failure conditions
- **Monitoring**: Real-time metrics and alerting

## 🛠️ Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- curl (for testing)

### Installation

1. **Generate the system**:
   ```bash
   chmod +x generate_system_files.sh
   ./generate_system_files.sh
   cd log-processing-system
   ```

2. **Start the system**:
   ```bash
   ./setup.sh
   ```

3. **Verify installation**:
   ```bash
   ./integration-tests/system-test.sh
   ```

## 📊 Usage

### Start Log Generation
```bash
curl -X POST http://localhost:8090/api/v1/generator/start
```

### Check Status
```bash
curl http://localhost:8090/api/v1/generator/status
```

### Stop Generation
```bash
curl -X POST http://localhost:8090/api/v1/generator/stop
```

## 🧪 Testing

### Integration Tests
```bash
./integration-tests/system-test.sh
```

### Load Testing
```bash
./load-test.sh
```

## 📈 Monitoring

### Available Dashboards
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Zipkin**: http://localhost:9411

### Key Metrics
- `log_events_generated_total`: Total events generated
- `log_events_rate_limited_total`: Events dropped due to rate limiting
- `log_generation_rate_current`: Current generation rate per second
- `kafka_messages_sent_total`: Messages successfully sent to Kafka
- `circuit_breaker_state`: Current circuit breaker state

## 🔧 Configuration

### Log Generator Settings
Edit `log-generator/src/main/resources/application.yml`:

```yaml
app:
  generator:
    threads: 4                    # Number of generation threads
    rate-per-second: 1000        # Target generation rate
    batch-size: 10               # Events per batch
    instance-id: generator-1     # Unique instance identifier
```

### Rate Limiting
```yaml
# Redis-based sliding window rate limiting
# Configurable per endpoint in application.yml
```

### Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      kafka-producer:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
```

## 🏗️ Architecture Deep Dive

### Event Generation Flow
1. **Thread Pool**: Configurable worker threads generate events
2. **Rate Limiting**: Redis sliding window prevents overload
3. **Batching**: Adaptive batch sizes optimize throughput
4. **Circuit Breaker**: Protects against Kafka failures
5. **Metrics**: Real-time performance monitoring

### Scalability Patterns
- **Horizontal Scaling**: Multiple generator instances coordinate via Redis
- **Load Balancing**: API Gateway distributes requests
- **Resource Management**: Automatic backpressure and flow control
- **Fault Tolerance**: Circuit breakers prevent cascading failures

## 🚨 Production Considerations

### Performance Tuning
- Adjust `app.generator.threads` based on CPU cores
- Optimize `app.generator.batch-size` for your Kafka cluster
- Monitor `log_generation_rate_current` vs target rate

### Monitoring & Alerting
- Set up alerts for circuit breaker open states
- Monitor memory usage and GC pressure
- Track rate limiting metrics for capacity planning

### Failure Scenarios
- **Kafka Unavailable**: Circuit breaker activates, graceful degradation
- **Redis Unavailable**: Falls back to local rate limiting
- **Memory Pressure**: Automatic load shedding maintains stability

## 🔄 Next Steps

Tomorrow (Day 3), we'll build the log collector service that consumes these events and implements stream processing patterns:
- Real-time event processing
- Windowing operations
- Stream aggregations
- Event filtering and routing

## 📚 Learning Outcomes

After completing this lesson, you can:
- ✅ Implement production-ready rate limiting with Redis
- ✅ Configure circuit breakers for fault tolerance  
- ✅ Design event sourcing patterns with Kafka
- ✅ Build observable systems with metrics and tracing
- ✅ Handle backpressure in distributed systems
- ✅ Scale event generation horizontally
- ✅ Monitor and tune system performance

## 🛑 Cleanup

```bash
./stop.sh
docker-compose down -v  # Remove volumes
```

---

**Built with**: Spring Boot 3.2, Apache Kafka, Redis, PostgreSQL, Prometheus, Grafana
**Pattern Focus**: Rate Limiting, Circuit Breakers, Event Sourcing, Observability
**Scale Target**: 50K+ events/second with horizontal scaling capability
