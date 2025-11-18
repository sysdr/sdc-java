# Prometheus Queries for Avro Log Processor

## Access Prometheus
Open http://localhost:9090 in your browser and use these queries in the Prometheus query interface.

---

## Event Metrics

### Total Events Produced
```promql
avro_events_produced_total{application="avro-log-producer"}
```

### Total Events Consumed
```promql
avro_events_consumed_total{application="avro-log-consumer"}
```

### Events Produced Rate (per second)
```promql
rate(avro_events_produced_total{application="avro-log-producer"}[5m])
```

### Events Consumed Rate (per second)
```promql
rate(avro_events_consumed_total{application="avro-log-consumer"}[5m])
```

### Events Produced in Last Hour
```promql
increase(avro_events_produced_total{application="avro-log-producer"}[1h])
```

### Events Consumed in Last Hour
```promql
increase(avro_events_consumed_total{application="avro-log-consumer"}[1h])
```

---

## Schema Version Metrics

### V1 Events Total
```promql
avro_events_v1_total{application="avro-log-consumer"}
```

### V2 Events Total
```promql
avro_events_v2_total{application="avro-log-consumer"}
```

### V1 Events Rate
```promql
rate(avro_events_v1_total{application="avro-log-consumer"}[5m])
```

### V2 Events Rate
```promql
rate(avro_events_v2_total{application="avro-log-consumer"}[5m])
```

### Schema Version Distribution (Percentage)
```promql
# V1 Percentage
(avro_events_v1_total{application="avro-log-consumer"} / avro_events_consumed_total{application="avro-log-consumer"}) * 100

# V2 Percentage
(avro_events_v2_total{application="avro-log-consumer"} / avro_events_consumed_total{application="avro-log-consumer"}) * 100
```

---

## Error Metrics

### Total Production Errors
```promql
avro_events_errors_total{application="avro-log-producer"}
```

### Total Processing Errors
```promql
avro_events_processing_errors_total{application="avro-log-consumer"}
```

### Production Error Rate
```promql
rate(avro_events_errors_total{application="avro-log-producer"}[5m])
```

### Processing Error Rate
```promql
rate(avro_events_processing_errors_total{application="avro-log-consumer"}[5m])
```

### Error Rate Percentage
```promql
# Production Error Rate %
(rate(avro_events_errors_total{application="avro-log-producer"}[5m]) / rate(avro_events_produced_total{application="avro-log-producer"}[5m])) * 100

# Processing Error Rate %
(rate(avro_events_processing_errors_total{application="avro-log-consumer"}[5m]) / rate(avro_events_consumed_total{application="avro-log-consumer"}[5m])) * 100
```

---

## Performance Metrics

### Average Production Time (seconds)
```promql
rate(avro_production_time_seconds_sum{application="avro-log-producer"}[5m]) / rate(avro_production_time_seconds_count{application="avro-log-producer"}[5m])
```

### Average Processing Time (seconds)
```promql
rate(avro_processing_time_seconds_sum{application="avro-log-consumer"}[5m]) / rate(avro_processing_time_seconds_count{application="avro-log-consumer"}[5m])
```

### 95th Percentile Production Time
```promql
histogram_quantile(0.95, rate(avro_production_time_seconds_bucket{application="avro-log-producer"}[5m]))
```

### 95th Percentile Processing Time
```promql
histogram_quantile(0.95, rate(avro_processing_time_seconds_bucket{application="avro-log-consumer"}[5m]))
```

### Max Production Time
```promql
avro_production_time_seconds_max{application="avro-log-producer"}
```

### Max Processing Time
```promql
avro_processing_time_seconds_max{application="avro-log-consumer"}
```

---

## Throughput Metrics

### Events Per Second (Combined)
```promql
sum(rate(avro_events_produced_total{application="avro-log-producer"}[5m]))
```

### Consumption Lag (if events are queued)
```promql
avro_events_produced_total{application="avro-log-producer"} - avro_events_consumed_total{application="avro-log-consumer"}
```

### Processing Throughput (events/second)
```promql
sum(rate(avro_events_consumed_total{application="avro-log-consumer"}[5m]))
```

---

## Health & Availability

### Service Uptime
```promql
up{job="log-producer"}
up{job="log-consumer"}
up{job="api-gateway"}
```

### JVM Memory Usage
```promql
# Producer
jvm_memory_used_bytes{application="avro-log-producer"}
jvm_memory_max_bytes{application="avro-log-producer"}

# Consumer
jvm_memory_used_bytes{application="avro-log-consumer"}
jvm_memory_max_bytes{application="avro-log-consumer"}
```

### HTTP Request Rate
```promql
rate(http_server_requests_seconds_count{application="avro-log-producer"}[5m])
rate(http_server_requests_seconds_count{application="avro-log-consumer"}[5m])
rate(http_server_requests_seconds_count{application="api-gateway"}[5m])
```

---

## Advanced Queries

### Event Production Trend (Last 1 Hour)
```promql
increase(avro_events_produced_total{application="avro-log-producer"}[1h])
```

### Event Consumption Trend (Last 1 Hour)
```promql
increase(avro_events_consumed_total{application="avro-log-consumer"}[1h])
```

### Success Rate
```promql
# Production Success Rate
(1 - (rate(avro_events_errors_total{application="avro-log-producer"}[5m]) / rate(avro_events_produced_total{application="avro-log-producer"}[5m]))) * 100

# Processing Success Rate
(1 - (rate(avro_events_processing_errors_total{application="avro-log-consumer"}[5m]) / rate(avro_events_consumed_total{application="avro-log-consumer"}[5m]))) * 100
```

### Total Events Across All Services
```promql
sum(avro_events_produced_total) + sum(avro_events_consumed_total)
```

### Average Events Per Minute
```promql
rate(avro_events_produced_total{application="avro-log-producer"}[1m]) * 60
rate(avro_events_consumed_total{application="avro-log-consumer"}[1m]) * 60
```

---

## Alerting Queries

### High Error Rate Alert (> 5% errors)
```promql
(rate(avro_events_errors_total{application="avro-log-producer"}[5m]) / rate(avro_events_produced_total{application="avro-log-producer"}[5m])) > 0.05
```

### Low Throughput Alert (< 1 event/second)
```promql
rate(avro_events_produced_total{application="avro-log-producer"}[5m]) < 1
```

### High Latency Alert (> 1 second)
```promql
rate(avro_production_time_seconds_sum{application="avro-log-producer"}[5m]) / rate(avro_production_time_seconds_count{application="avro-log-producer"}[5m]) > 1
```

### Service Down Alert
```promql
up{job=~"log-producer|log-consumer|api-gateway"} == 0
```

---

## Quick Reference

### Most Common Queries

1. **Current Event Counts:**
   ```promql
   avro_events_produced_total{application="avro-log-producer"}
   avro_events_consumed_total{application="avro-log-consumer"}
   ```

2. **Event Rates:**
   ```promql
   rate(avro_events_produced_total{application="avro-log-producer"}[5m])
   rate(avro_events_consumed_total{application="avro-log-consumer"}[5m])
   ```

3. **Error Counts:**
   ```promql
   avro_events_errors_total{application="avro-log-producer"}
   avro_events_processing_errors_total{application="avro-log-consumer"}
   ```

4. **Schema Version Distribution:**
   ```promql
   avro_events_v1_total{application="avro-log-consumer"}
   avro_events_v2_total{application="avro-log-consumer"}
   ```

---

## Usage Tips

1. **Time Range:** Adjust the time range in Prometheus UI (e.g., [5m], [1h], [1d])
2. **Graph View:** Click "Graph" tab to visualize metrics over time
3. **Table View:** Click "Table" tab to see current values
4. **Export:** Use these queries in Grafana dashboards for visualization

---

## Grafana Dashboard Queries

Copy these queries into Grafana panels:

### Panel: Events Produced (Graph)
```promql
rate(avro_events_produced_total{application="avro-log-producer"}[5m])
```

### Panel: Events Consumed (Graph)
```promql
rate(avro_events_consumed_total{application="avro-log-consumer"}[5m])
```

### Panel: Error Rate (Graph)
```promql
rate(avro_events_errors_total{application="avro-log-producer"}[5m]) + rate(avro_events_processing_errors_total{application="avro-log-consumer"}[5m])
```

### Panel: Schema Version Distribution (Pie Chart)
```promql
avro_events_v1_total{application="avro-log-consumer"}
avro_events_v2_total{application="avro-log-consumer"}
```

