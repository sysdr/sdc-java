# Monitoring & Observability Setup

This directory contains comprehensive monitoring and observability configurations for the TLS-secured distributed log processing system.

## 📊 Dashboards

### 1. Log Processing System - Comprehensive Dashboard
**File:** `dashboards/log-processing-dashboard.json`

**Overview:** Main system dashboard providing a complete view of the entire log processing pipeline.

**Key Metrics:**
- Service health status (API Gateway, Log Producer, Log Consumer, Kafka, PostgreSQL, Redis)
- Request throughput (RPS) across all services
- Response time percentiles (p95) for performance monitoring
- JVM memory usage and garbage collection activity
- CPU usage per service
- Kafka producer and consumer metrics
- Circuit breaker states and failure rates
- Database connection pool metrics
- Redis cache performance
- TLS handshake duration
- Error rates by service
- Log level distribution
- System load average

### 2. Kafka Performance Dashboard
**File:** `dashboards/kafka-dashboard.json`

**Overview:** Specialized dashboard focused on Kafka cluster performance and message flow.

**Key Metrics:**
- Kafka cluster health and broker state
- Topic throughput (messages and bytes in/out per second)
- Consumer lag monitoring
- Producer and consumer metrics
- Partition distribution and lag per partition
- Kafka JVM metrics
- Network I/O performance
- Compression ratio analysis

### 3. TLS Security Dashboard
**File:** `dashboards/tls-security-dashboard.json`

**Overview:** Security-focused dashboard monitoring TLS connections and certificate health.

**Key Metrics:**
- TLS connection status across all services
- TLS handshake duration (p50, p95, p99)
- TLS error rates (4xx, 5xx)
- Certificate health status
- Certificate expiration timeline and days until expiration
- TLS cipher suites and protocol versions
- TLS connection counts (active/total)
- TLS handshake failures and certificate validation errors
- TLS performance impact analysis

### 4. Database & Cache Performance Dashboard
**File:** `dashboards/database-cache-dashboard.json`

**Overview:** Database and caching layer performance monitoring.

**Key Metrics:**
- PostgreSQL and Redis health status
- PostgreSQL connection pool metrics (active, idle, total, pending)
- Database query performance and latency
- Redis cache performance (commands/sec, hit/miss ratio)
- Redis memory usage and key expiration
- Log processing success/error rates
- Cache hit/miss ratio and log level distribution
- Database storage growth over time
- Connection pool efficiency metrics

### 5. Resilience & Circuit Breaker Dashboard
**File:** `dashboards/resilience-dashboard.json`

**Overview:** Resilience patterns and circuit breaker monitoring.

**Key Metrics:**
- Circuit breaker states (CLOSED, OPEN, HALF_OPEN)
- Circuit breaker failure rates and call counts
- Retry attempt metrics and configuration
- Bulkhead metrics (available/max concurrent calls)
- Time limiter metrics (success/timeout rates)
- Rate limiter metrics (permissions, waiting threads)
- Overall system resilience score

## 🚨 Alerting Rules

**File:** `alerting-rules.yml`

### Critical Alerts (Immediate Response Required)
- **ServiceDown:** Any service (API Gateway, Log Producer, Log Consumer) is down
- **KafkaDown:** Kafka broker is down
- **DatabaseDown:** PostgreSQL is down
- **CircuitBreakerOpen:** Circuit breaker is in OPEN state
- **CertificateExpired:** TLS certificate has expired

### Warning Alerts (Monitor and Investigate)
- **HighErrorRate:** Error rate > 5% for any service
- **HighResponseTime:** 95th percentile response time > 1 second
- **HighMemoryUsage:** JVM heap usage > 85%
- **HighCPUUsage:** CPU usage > 80%
- **KafkaConsumerLag:** Consumer lag > 1000 messages
- **CertificateExpiringSoon:** Certificate expires within 7 days
- **DatabaseConnectionPoolExhausted:** Connection pool utilization > 90%

### Performance Alerts
- **HighSystemLoad:** System load average > 4
- **DiskSpaceLow:** Available disk space < 10%
- **LogProcessingErrors:** Log processing error rate > 0.1 errors/sec
- **GatewayRequestFailures:** Gateway failure rate > 10%

## 🔧 Configuration Files

### Prometheus Configuration
**File:** `prometheus.yml`

- **Scrape Interval:** 15 seconds
- **Evaluation Interval:** 15 seconds
- **Targets:** All Spring Boot services, Kafka, PostgreSQL, Redis, Node Exporter
- **Alerting Rules:** Loaded from `alerting-rules.yml`
- **Alert Manager:** Configured for alert routing

### Grafana Dashboard Provisioning
**File:** `grafana-dashboards.yml`

- **Provider:** File-based dashboard provisioning
- **Path:** `/var/lib/grafana/dashboards`
- **Auto-loading:** All JSON dashboards are automatically loaded

## 🚀 Getting Started

### 1. Start the Monitoring Stack
```bash
# Start infrastructure and monitoring services
docker-compose up -d zookeeper kafka postgres redis prometheus grafana

# Wait for services to be ready
sleep 30

# Start Spring Boot applications
cd log-producer && mvn spring-boot:run &
cd ../log-consumer && mvn spring-boot:run &
cd ../api-gateway && mvn spring-boot:run &
```

### 2. Access Monitoring Interfaces

- **Grafana:** http://localhost:3000 (admin/admin)
- **Prometheus:** http://localhost:9090
- **Alert Manager:** http://localhost:9093 (if configured)

### 3. Import Dashboards

The dashboards are automatically provisioned when Grafana starts. You can also manually import them:

1. Go to Grafana → Dashboards → Import
2. Upload the JSON files from the `dashboards/` directory
3. Select the Prometheus data source

## 📈 Key Performance Indicators (KPIs)

### System Health
- **Uptime:** All services should show `up=1`
- **Response Time:** p95 < 500ms for normal operations
- **Error Rate:** < 1% for all services
- **Memory Usage:** < 80% of allocated heap

### Kafka Performance
- **Throughput:** > 1000 messages/sec
- **Consumer Lag:** < 100 messages
- **Producer Success Rate:** > 99%

### Database Performance
- **Connection Pool Utilization:** < 80%
- **Query Latency:** p95 < 100ms
- **Cache Hit Rate:** > 90%

### Security
- **Certificate Health:** All certificates healthy
- **TLS Handshake Time:** < 100ms
- **TLS Error Rate:** < 0.1%

## 🔍 Troubleshooting

### Common Issues

1. **Dashboard Not Loading Data**
   - Check if Prometheus is scraping metrics from services
   - Verify service endpoints are accessible
   - Check for TLS certificate issues

2. **Alerts Not Firing**
   - Verify alerting rules are loaded in Prometheus
   - Check alert manager configuration
   - Review alert thresholds

3. **Missing Metrics**
   - Ensure Spring Boot Actuator is enabled
   - Check Prometheus scrape configuration
   - Verify service discovery

### Debug Commands

```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Check alerting rules
curl http://localhost:9090/api/v1/rules

# Check service health
curl -k https://localhost:8080/actuator/health
curl -k https://localhost:8081/actuator/health
curl -k https://localhost:8082/actuator/health

# Check metrics endpoint
curl -k https://localhost:8080/actuator/prometheus
```

## 📚 Additional Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Resilience4j Metrics](https://resilience4j.readme.io/docs/micrometer)
- [Kafka Monitoring](https://kafka.apache.org/documentation/#monitoring)
