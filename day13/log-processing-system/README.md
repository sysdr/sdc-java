# Day 13: TLS-Secured Distributed Log Processing System

A production-ready distributed log processing system with mutual TLS (mTLS) authentication, built with Spring Boot 3.2+, Apache Kafka, PostgreSQL, and Redis.

## 🏗️ Architecture Overview

```
┌─────────────┐      HTTPS/TLS     ┌──────────────┐      Kafka/TLS      ┌──────────────┐
│             │ ───────────────────>│              │ ──────────────────> │              │
│ API Gateway │                     │ Log Producer │                     │    Kafka     │
│   :8080     │                     │    :8081     │                     │    :9093     │
└─────────────┘                     └──────────────┘                     └──────┬───────┘
                                                                                 │
                                                                                 │ TLS
                                                                                 │
                                                                                 v
┌──────────────┐                    ┌──────────────┐                     ┌─────────────┐
│              │                    │              │<────────────────────│             │
│  Prometheus  │<───────────────────│ Log Consumer │                     │   Redis     │
│    :9090     │                    │    :8082     │────────────────────>│   :6379     │
└──────────────┘                    └──────────────┘                     └─────────────┘
                                            │
                                            │ JDBC
                                            v
                                    ┌──────────────┐
                                    │  PostgreSQL  │
                                    │    :5432     │
                                    └──────────────┘
```

### Components

1. **API Gateway** (:8080)
   - Entry point for external requests
   - Circuit breaker pattern with Resilience4j
   - Request routing and load balancing
   - Metrics and health endpoints

2. **Log Producer** (:8081)
   - Receives log events via REST API
   - Publishes to Kafka with LZ4 compression
   - Certificate health monitoring
   - Prometheus metrics export

3. **Log Consumer** (:8082)
   - Consumes from Kafka topics
   - Persists to PostgreSQL
   - Caches critical logs in Redis
   - Error handling and retry logic

4. **Apache Kafka** (:9093)
   - Message broker with TLS encryption
   - 3 partitions for parallelism
   - Inter-broker and client-broker TLS

5. **PostgreSQL** (:5432)
   - Persistent log storage
   - Indexed queries on timestamp, level, source

6. **Redis** (:6379)
   - Cache for recent ERROR/WARN logs
   - 1-hour TTL

7. **Simple Dashboard** (:3001)
   - Lightweight web-based monitoring
   - Real-time service health monitoring
   - Interactive charts and metrics
   - No external dependencies

## 🔐 TLS Security Features

- **Mutual TLS (mTLS)** between all services
- **Certificate hierarchy**: Root CA → Intermediate CA → Service certificates
- **30-day certificate validity** with automated rotation support
- **Certificate health checks** (alerts 7 days before expiration)
- **TLS 1.3** enabled for optimized handshakes
- **Hostname verification** to prevent impersonation attacks

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- OpenSSL (for certificate generation)

### Setup (5 minutes)

```bash
# 1. Generate all project files
chmod +x generate_system_files.sh
./generate_system_files.sh

# 2. Navigate to project directory
cd log-processing-system

# 3. Run setup script (generates certs, starts infrastructure, builds services)
chmod +x setup.sh
./setup.sh

# This will:
# - Generate TLS certificates for all services
# - Start Kafka, PostgreSQL, Redis, Simple Dashboard
# - Build Maven projects
# - Configure keystores and truststores
```

### Start Services

```bash
# Terminal 1: API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 2: Log Producer
cd log-producer
mvn spring-boot:run

# Terminal 3: Log Consumer
cd log-consumer
mvn spring-boot:run
```

### Verify System Health

```bash
# Check all services are up
curl -k https://localhost:8080/api/v1/health    # Gateway
curl -k https://localhost:8081/api/logs/health   # Producer
curl -k https://localhost:8082/actuator/health   # Consumer

# View Kafka topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

## 📊 Testing & Load Generation

### Integration Tests

```bash
# Run integration test suite
cd integration-tests
./test-tls-connection.sh

# Run JUnit tests
mvn test
```

### Load Testing

```bash
# Generate load (1000 requests, 10 concurrent)
./load-test.sh

# Expected throughput with TLS:
# - ~500-800 req/sec (single node)
# - p99 latency: 50-100ms
# - TLS overhead: ~20-25%
```

### Manual Testing

```bash
# Send a log event via gateway
curl -k -X POST https://localhost:8080/api/v1/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR",
    "message": "Database connection timeout",
    "source": "payment-service",
    "metadata": {
      "userId": "12345",
      "endpoint": "/api/checkout"
    }
  }'

# Direct producer access
curl -k -X POST https://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "WARN",
    "message": "High memory usage detected",
    "source": "monitoring-agent",
    "metadata": {"memoryUsage": "85%"}
  }'
```

## 📈 Monitoring & Observability

### Simple Dashboard (http://localhost:3001)

A lightweight, self-contained monitoring solution that provides:

**Real-time Service Health**
- API Gateway, Log Producer, Log Consumer status
- Infrastructure health (Kafka, PostgreSQL, Redis)
- Color-coded status indicators

**System Metrics**
- Total request count and error rates
- Memory usage and performance metrics
- Real-time updates every 5 seconds

**Interactive Charts**
- Request throughput over time
- Response time percentiles (p95)
- JVM memory usage trends
- Error rate monitoring

**API Endpoints**
- `GET /api/metrics` - All collected metrics
- `GET /api/health` - Service health status
- `GET /api/aggregated` - System-wide metrics
- `GET /api/timeseries` - Chart data

### Logs

```bash
# View all service logs
docker-compose logs -f

# Filter by service
docker-compose logs -f kafka
docker-compose logs -f postgres

# Application logs
tail -f log-producer/logs/application.log
```

## 🔧 Configuration

### Environment Variables

Create `.env` file in project root:

```bash
# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9093

# Database
DB_HOST=postgres
DB_NAME=logdb
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=redis

# TLS Certificates
KEYSTORE_PATH=/etc/ssl/certs/keystore.jks
TRUSTSTORE_PATH=/etc/ssl/certs/truststore.jks
KEYSTORE_PASSWORD=changeit
TRUSTSTORE_PASSWORD=changeit
KEY_PASSWORD=changeit
```

### Certificate Rotation

```bash
# Regenerate certificates (auto-rotates on startup)
cd certs
./generate-certs.sh

# Restart services to load new certificates
docker-compose restart

# Verify new certificates
echo | openssl s_client -connect localhost:8080 -showcerts 2>/dev/null | \
  openssl x509 -noout -dates
```

## 🎯 Performance Benchmarks

### Baseline (Day 12 - Compression only)
- Throughput: 1000 req/sec
- p99 Latency: 25ms
- Network: 5 MB/s

### Day 13 (TLS + Compression)
- Throughput: 750 req/sec (-25%)
- p99 Latency: 40ms (+60%)
- CPU Usage: +15%
- Network: 5 MB/s (unchanged, compression effective)

### TLS Optimizations Applied
- TLS 1.3 (1-RTT handshakes)
- Session resumption (10-minute cache)
- Connection pooling (2000 connections)
- AES-NI hardware acceleration

## 🐛 Troubleshooting

### Certificate Errors

```bash
# Verify certificate chain
openssl verify -CAfile certs/ca-chain.pem certs/log-producer-cert.pem

# Check certificate expiration
keytool -list -v -keystore certs/keystore.jks -storepass changeit

# Test TLS connection
openssl s_client -connect localhost:8080 -CAfile certs/ca-chain.pem
```

### Kafka Connection Issues

```bash
# Check Kafka logs
docker-compose logs kafka | grep -i ssl

# Verify Kafka TLS config
docker-compose exec kafka kafka-configs --bootstrap-server localhost:9092 \
  --entity-type brokers --entity-name 0 --describe --all
```

### Service Won't Start

```bash
# Check port conflicts
lsof -i :8080
lsof -i :8081
lsof -i :8082

# Verify dependencies
docker-compose ps

# Check service logs
mvn spring-boot:run -X  # Debug mode
```

## 📚 Next Steps: Day 14

Tomorrow, we'll build a sophisticated load generator to:
- Measure system saturation points
- Identify performance bottlenecks
- Calculate capacity planning metrics
- Validate horizontal scaling strategies

### Pre-work for Day 14
1. Complete today's system and run load tests
2. Identify your system's throughput limit
3. Note any performance degradation patterns
4. Review the simple dashboard for bottlenecks

## 🔗 Resources

- [Spring Boot SSL Bundle Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.ssl)
- [Apache Kafka Security Guide](https://kafka.apache.org/documentation/#security)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [OpenSSL Certificate Management](https://www.openssl.org/docs/)

## 📝 License

MIT License - Educational purposes for 254-Day System Design Course

---

**Course Context**: Day 13 of 254 | Module 1: Foundations of Log Processing | Week 1: Setting Up the Infrastructure

Built with ❤️ for learning distributed systems at scale.
