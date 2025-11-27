# Monitoring Guide: Prometheus & Grafana

This guide explains how to access and view metrics in Prometheus and Grafana for the Log Format Compatibility Layer.

## Access URLs

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000
  - Username: `admin`
  - Password: `admin`

## Available Metrics

### Service-Specific Metrics

#### Syslog Adapter (Port 8081)
- `syslog_messages_produced_total` - Total syslog messages ingested
- `syslog_producer_errors_total` - Producer errors

#### Journald Adapter (Port 8082)
- `journald_messages_produced_total` - Total journald messages ingested

#### Format Normalizer (Port 8083)
- `normalizer_events_processed_total` - Total normalized events
- `normalizer_errors_total` - Normalization failures

#### API Gateway (Port 8080)
- Kafka consumer metrics
- Redis connection metrics
- JVM metrics

### Standard Metrics (All Services)
- `jvm_memory_used_bytes` - Memory usage
- `jvm_memory_max_bytes` - Max memory
- `jvm_gc_pause_seconds` - Garbage collection metrics
- `http_server_requests_seconds` - HTTP request metrics
- `kafka_consumer_*` - Kafka consumer metrics
- `kafka_producer_*` - Kafka producer metrics

## Using Prometheus

### 1. Access Prometheus UI
Open http://localhost:9090 in your browser.

### 2. Check Targets Status
- Go to **Status → Targets**
- Verify all services show as "UP" (green)
- If any show "DOWN", check:
  - Service is running
  - Port is accessible
  - Network connectivity

### 3. Query Metrics

#### Example Queries:

**Total syslog messages:**
```
syslog_messages_produced_total
```

**Total normalized events:**
```
normalizer_events_processed_total
```

**Normalization error rate:**
```
rate(normalizer_errors_total[5m])
```

**Memory usage by service:**
```
jvm_memory_used_bytes{area="heap"}
```

**Kafka consumer lag:**
```
kafka_consumer_lag_sum
```

**HTTP request rate:**
```
rate(http_server_requests_seconds_count[5m])
```

**Messages per second:**
```
rate(syslog_messages_produced_total[1m]) + rate(journald_messages_produced_total[1m])
```

### 4. Create Graphs
- Go to **Graph** tab
- Enter a PromQL query
- Click **Execute**
- View the graph or switch to **Graph** tab for visualization

### 5. View Metrics Endpoint Directly
You can also access metrics directly from each service:
- Syslog Adapter: http://localhost:8081/actuator/prometheus
- Journald Adapter: http://localhost:8082/actuator/prometheus
- Format Normalizer: http://localhost:8083/actuator/prometheus
- API Gateway: http://localhost:8080/actuator/prometheus

## Using Grafana

### 1. Access Grafana
Open http://localhost:3000 and login with:
- Username: `admin`
- Password: `admin`

### 2. Add Prometheus Data Source
1. Go to **Configuration → Data Sources**
2. Click **Add data source**
3. Select **Prometheus**
4. Set URL: `http://prometheus:9090` (or `http://localhost:9090` if accessing from host)
5. Click **Save & Test**

### 3. Create Dashboards

#### Quick Dashboard Creation:
1. Go to **Dashboards → New Dashboard**
2. Click **Add visualization**
3. Select Prometheus data source
4. Enter a query (e.g., `syslog_messages_produced_total`)
5. Configure visualization type (Graph, Stat, etc.)
6. Save the panel

#### Recommended Panels:

**System Overview:**
- Total Messages: `syslog_messages_produced_total + journald_messages_produced_total`
- Normalized Events: `normalizer_events_processed_total`
- Error Rate: `rate(normalizer_errors_total[5m])`

**Service Health:**
- Memory Usage: `jvm_memory_used_bytes{area="heap"}`
- HTTP Request Rate: `rate(http_server_requests_seconds_count[5m])`
- Kafka Consumer Lag: `kafka_consumer_lag_sum`

**Throughput:**
- Messages/Second: `rate(syslog_messages_produced_total[1m]) + rate(journald_messages_produced_total[1m])`
- Normalization Rate: `rate(normalizer_events_processed_total[1m])`

### 4. Import Pre-built Dashboard (Optional)
You can import a JSON dashboard configuration if available.

## Troubleshooting

### Prometheus Targets Show as DOWN

1. **Check if services are running:**
   ```bash
   ps aux | grep java
   ```

2. **Verify metrics endpoints:**
   ```bash
   curl http://localhost:8080/actuator/prometheus
   curl http://localhost:8081/actuator/prometheus
   curl http://localhost:8082/actuator/prometheus
   curl http://localhost:8083/actuator/prometheus
   ```

3. **Check Prometheus configuration:**
   - File: `monitoring/prometheus.yml`
   - Ensure targets use correct host/IP
   - In WSL2, may need to use host IP instead of `host.docker.internal`

4. **Restart Prometheus:**
   ```bash
   docker restart log-format-compatibility-prometheus-1
   ```

### No Metrics Appearing

1. **Verify services are generating metrics:**
   - Check service logs for errors
   - Ensure services are processing messages

2. **Check scrape interval:**
   - Prometheus scrapes every 15 seconds by default
   - Wait a few minutes for metrics to appear

3. **Verify metric names:**
   - Check actual metric names in `/actuator/prometheus` endpoint
   - Use exact metric names in queries

### Grafana Can't Connect to Prometheus

1. **Check Prometheus is accessible:**
   ```bash
   curl http://localhost:9090/api/v1/status/config
   ```

2. **Verify data source URL:**
   - From Grafana container: `http://prometheus:9090`
   - From host: `http://localhost:9090`

3. **Check network connectivity:**
   ```bash
   docker exec log-format-compatibility-grafana-1 ping prometheus
   ```

## Useful PromQL Queries

### Rate Queries
```promql
# Messages per second
rate(syslog_messages_produced_total[1m])

# Error rate
rate(normalizer_errors_total[5m])

# HTTP requests per second
rate(http_server_requests_seconds_count[5m])
```

### Aggregation Queries
```promql
# Total messages across all services
sum(syslog_messages_produced_total + journald_messages_produced_total)

# Average memory usage
avg(jvm_memory_used_bytes{area="heap"})

# Max consumer lag
max(kafka_consumer_lag_sum)
```

### Time Range Queries
```promql
# Messages in last hour
increase(syslog_messages_produced_total[1h])

# Error count in last 5 minutes
increase(normalizer_errors_total[5m])
```

## Quick Reference

### Service Endpoints
- Syslog Adapter Metrics: http://localhost:8081/actuator/prometheus
- Journald Adapter Metrics: http://localhost:8082/actuator/prometheus
- Format Normalizer Metrics: http://localhost:8083/actuator/prometheus
- API Gateway Metrics: http://localhost:8080/actuator/prometheus

### Key Metrics
- `syslog_messages_produced_total` - Syslog message count
- `journald_messages_produced_total` - Journald message count
- `normalizer_events_processed_total` - Normalized events count
- `normalizer_errors_total` - Normalization errors
- `jvm_memory_used_bytes` - Memory usage
- `kafka_consumer_lag_sum` - Consumer lag

### Useful Links
- Prometheus Query Documentation: https://prometheus.io/docs/prometheus/latest/querying/basics/
- Grafana Dashboard Documentation: https://grafana.com/docs/grafana/latest/dashboards/

