# Prometheus Queries Guide

This guide provides useful PromQL queries for monitoring your Log Cluster System.

## 📊 Cluster Health Metrics

### Total Heartbeats Sent (Rate)
```promql
rate(cluster_heartbeats_sent_total[5m])
```
Shows the rate of heartbeats being sent per second over the last 5 minutes.

### Total Heartbeats Received (Rate)
```promql
rate(cluster_heartbeats_received_total[5m])
```
Shows the rate of heartbeats being received per second.

### Node Failures Detected
```promql
cluster_failures_detected_total
```
Total count of node failures detected since startup.

### Node Failures Rate
```promql
rate(cluster_failures_detected_total[5m])
```
Rate of node failures detected per second.

### Heartbeat Success Rate
```promql
rate(cluster_heartbeats_received_total[5m]) / rate(cluster_heartbeats_sent_total[5m]) * 100
```
Percentage of heartbeats successfully received.

### Service Availability (Up/Down)
```promql
up
```
Returns 1 if the service is up, 0 if down. Shows status of all scraped targets.

### Service Availability by Job
```promql
up{job="cluster-coordinator"}
up{job="log-producer"}
up{job="log-consumer"}
up{job="api-gateway"}
```

## 🖥️ JVM Metrics

### Memory Usage (Heap)
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```
Percentage of heap memory used.

### Memory Usage by Pool
```promql
jvm_memory_used_bytes{area="heap", id=~"G1.*"}
```
Memory usage for G1 garbage collector pools.

### GC Pause Time
```promql
rate(jvm_gc_pause_seconds_sum[5m])
```
Total GC pause time per second.

### GC Pause Count
```promql
rate(jvm_gc_pause_seconds_count[5m])
```
Number of GC pauses per second.

### Thread States
```promql
jvm_threads_states_threads{state="runnable"}
jvm_threads_states_threads{state="blocked"}
jvm_threads_states_threads{state="waiting"}
```
Number of threads in different states.

### Total Threads
```promql
sum(jvm_threads_states_threads)
```
Total number of JVM threads.

## 🌐 HTTP Metrics

### HTTP Request Rate
```promql
rate(http_server_requests_seconds_count[5m])
```
Requests per second by endpoint.

### HTTP Request Rate by Status Code
```promql
rate(http_server_requests_seconds_count{status=~"2.."}[5m])
rate(http_server_requests_seconds_count{status=~"4.."}[5m])
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```
Request rates for 2xx, 4xx, and 5xx responses.

### HTTP Request Latency (p95)
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```
95th percentile request latency.

### HTTP Request Latency (p99)
```promql
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```
99th percentile request latency.

### Active HTTP Requests
```promql
http_server_requests_active_seconds_active_count
```
Currently active HTTP requests.

### Average Response Time
```promql
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```
Average HTTP response time.

## 📨 Kafka Metrics

### Kafka Message Send Rate
```promql
rate(spring_kafka_template_seconds_count[5m])
```
Rate of messages sent to Kafka.

### Kafka Send Latency
```promql
rate(spring_kafka_template_seconds_sum[5m]) / rate(spring_kafka_template_seconds_count[5m])
```
Average Kafka send latency.

### Kafka Send Errors
```promql
rate(spring_kafka_template_seconds_count{exception!="none"}[5m])
```
Rate of Kafka send errors.

## 🔄 Redis Metrics

### Redis Command Rate
```promql
rate(lettuce_command_firstresponse_seconds_count[5m])
```
Rate of Redis commands executed.

### Redis Command Latency
```promql
rate(lettuce_command_firstresponse_seconds_sum[5m]) / rate(lettuce_command_firstresponse_seconds_count[5m])
```
Average Redis command latency.

### Redis Commands by Type
```promql
rate(lettuce_command_firstresponse_seconds_count[5m]) by (command)
```
Command rate grouped by command type (GET, SET, etc.).

## 📈 System Metrics

### Process Uptime
```promql
process_uptime_seconds
```
Uptime in seconds for each service.

### CPU Usage (if available)
```promql
process_cpu_usage
```
CPU usage percentage.

### Disk Free Space
```promql
disk_free_bytes / 1024 / 1024 / 1024
```
Free disk space in GB.

## 🎯 Composite Queries

### Cluster Health Score
```promql
# If you have a health score metric
cluster_health_score
```

### Service Availability Across All Services
```promql
sum(up) by (job)
```
Number of healthy instances per service.

### Total Requests Across All Services
```promql
sum(rate(http_server_requests_seconds_count[5m])) by (job)
```
Total request rate per service.

### Error Rate Percentage
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100
```
Percentage of requests that result in 5xx errors.

### Memory Usage Across All Services
```promql
sum(jvm_memory_used_bytes{area="heap"}) by (job) / sum(jvm_memory_max_bytes{area="heap"}) by (job) * 100
```
Heap memory usage percentage per service.

## 🔔 Alerting Queries

### High Error Rate Alert
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) > 0.1
```
Alert when error rate exceeds 0.1 errors/second.

### High Memory Usage Alert
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
```
Alert when heap memory usage exceeds 90%.

### Service Down Alert
```promql
up == 0
```
Alert when any service is down.

### Node Failure Alert
```promql
increase(cluster_failures_detected_total[5m]) > 0
```
Alert when node failures are detected.

### High GC Pause Time Alert
```promql
rate(jvm_gc_pause_seconds_sum[5m]) > 0.1
```
Alert when GC pause time exceeds 0.1 seconds/second.

## 📝 Quick Reference

### View All Available Metrics
```promql
{__name__=~".+"}
```

### List All Metric Names
In Prometheus UI, go to: Status → Targets → Select a target → Show more → Metrics

### Common Time Ranges
- `[5m]` - Last 5 minutes
- `[15m]` - Last 15 minutes
- `[1h]` - Last hour
- `[24h]` - Last 24 hours

### Common Functions
- `rate()` - Per-second average rate
- `increase()` - Total increase over time range
- `sum()` - Sum of values
- `avg()` - Average of values
- `max()` - Maximum value
- `min()` - Minimum value
- `histogram_quantile()` - Calculate quantiles from histograms

## 🎨 Grafana Dashboard Queries

### Cluster Overview Panel
```promql
# Total nodes
count(up{job=~"cluster-coordinator|log-producer|log-consumer|api-gateway"})

# Healthy nodes
sum(up{job=~"cluster-coordinator|log-producer|log-consumer|api-gateway"})

# Heartbeat rate
sum(rate(cluster_heartbeats_sent_total[5m]))
```

### Service Status Panel
```promql
up{job="cluster-coordinator"}
up{job="log-producer"}
up{job="log-consumer"}
up{job="api-gateway"}
```

### Request Rate Panel
```promql
sum(rate(http_server_requests_seconds_count[5m])) by (job)
```

### Memory Usage Panel
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

