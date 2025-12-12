# Prometheus & Grafana Queries for Quorum Log Storage

## Prometheus Queries

### API Gateway Metrics

#### Request Rate (Requests per Second)
```promql
# Total request rate
rate(http_server_requests_seconds_count{job="api-gateway"}[5m])

# Write requests rate
rate(http_server_requests_seconds_count{job="api-gateway", uri="/api/logs", method="POST"}[5m])

# Read requests rate
rate(http_server_requests_seconds_count{job="api-gateway", uri="/api/logs/{key}", method="GET"}[5m])
```

#### Request Latency
```promql
# Average latency (milliseconds)
rate(http_server_requests_seconds_sum{job="api-gateway"}[5m]) / rate(http_server_requests_seconds_count{job="api-gateway"}[5m]) * 1000

# 95th percentile latency (milliseconds)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="api-gateway"}[5m])) * 1000

# 99th percentile latency (milliseconds)
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{job="api-gateway"}[5m])) * 1000

# Write latency by consistency level
rate(http_server_requests_seconds_sum{job="api-gateway", uri="/api/logs", method="POST"}[5m]) / 
rate(http_server_requests_seconds_count{job="api-gateway", uri="/api/logs", method="POST"}[5m]) * 1000
```

#### Error Rate
```promql
# Error rate (4xx and 5xx)
rate(http_server_requests_seconds_count{job="api-gateway", status=~"4..|5.."}[5m])

# Error percentage
(rate(http_server_requests_seconds_count{job="api-gateway", status=~"4..|5.."}[5m]) / 
 rate(http_server_requests_seconds_count{job="api-gateway"}[5m])) * 100

# Success rate (2xx)
rate(http_server_requests_seconds_count{job="api-gateway", status=~"2.."}[5m])
```

### Quorum Coordinator Metrics

#### Quorum Operations Rate
```promql
# Write operations rate
rate(http_server_requests_seconds_count{job="quorum-coordinator", uri="/quorum/write"}[5m])

# Read operations rate
rate(http_server_requests_seconds_count{job="quorum-coordinator", uri="/quorum/read"}[5m])
```

#### Quorum Latency
```promql
# Average write latency
rate(http_server_requests_seconds_sum{job="quorum-coordinator", uri="/quorum/write"}[5m]) / 
rate(http_server_requests_seconds_count{job="quorum-coordinator", uri="/quorum/write"}[5m]) * 1000

# Average read latency
rate(http_server_requests_seconds_sum{job="quorum-coordinator", uri="/quorum/read"}[5m]) / 
rate(http_server_requests_seconds_count{job="quorum-coordinator", uri="/quorum/read"}[5m]) * 1000

# 95th percentile write latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="quorum-coordinator", uri="/quorum/write"}[5m])) * 1000
```

#### Quorum Success/Failure
```promql
# Successful quorum operations
rate(http_server_requests_seconds_count{job="quorum-coordinator", status="200"}[5m])

# Failed quorum operations (503 Service Unavailable)
rate(http_server_requests_seconds_count{job="quorum-coordinator", status="503"}[5m])

# Quorum success rate percentage
(rate(http_server_requests_seconds_count{job="quorum-coordinator", status="200"}[5m]) / 
 rate(http_server_requests_seconds_count{job="quorum-coordinator"}[5m])) * 100
```

#### Circuit Breaker Metrics (Resilience4j)
```promql
# Circuit breaker state (0=closed, 1=open, 2=half-open)
resilience4j_circuitbreaker_state{job="quorum-coordinator", name="storageNode"}

# Circuit breaker calls (total)
resilience4j_circuitbreaker_calls{job="quorum-coordinator", name="storageNode", kind="successful"}

# Circuit breaker failures
resilience4j_circuitbreaker_calls{job="quorum-coordinator", name="storageNode", kind="failed"}

# Circuit breaker failure rate
rate(resilience4j_circuitbreaker_calls{job="quorum-coordinator", name="storageNode", kind="failed"}[5m]) / 
rate(resilience4j_circuitbreaker_calls{job="quorum-coordinator", name="storageNode"}[5m]) * 100
```

### Storage Node Metrics

#### Per-Node Request Rate
```promql
# Write requests per node
rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/write"}[5m])

# Read requests per node
rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/read/{key}"}[5m])

# Total requests per node
rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])
```

#### Per-Node Latency
```promql
# Average latency per node
rate(http_server_requests_seconds_sum{job="storage-nodes"}[5m]) / 
rate(http_server_requests_seconds_count{job="storage-nodes"}[5m]) * 1000

# Write latency per node
rate(http_server_requests_seconds_sum{job="storage-nodes", uri="/storage/write"}[5m]) / 
rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/write"}[5m]) * 1000
```

#### Storage Node Health
```promql
# Node availability (based on successful requests)
(rate(http_server_requests_seconds_count{job="storage-nodes", status="200"}[5m]) / 
 rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])) * 100

# Node error rate
rate(http_server_requests_seconds_count{job="storage-nodes", status=~"4..|5.."}[5m])
```

### System Metrics

#### JVM Metrics
```promql
# Memory usage (heap)
jvm_memory_used_bytes{area="heap", job=~"api-gateway|quorum-coordinator|storage-nodes"}

# Memory max (heap)
jvm_memory_max_bytes{area="heap", job=~"api-gateway|quorum-coordinator|storage-nodes"}

# Memory usage percentage
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# GC pause time
rate(jvm_gc_pause_seconds_sum[5m])

# Thread count
jvm_threads_live_threads{job=~"api-gateway|quorum-coordinator|storage-nodes"}
```

#### Process Metrics
```promql
# CPU usage
rate(process_cpu_seconds_total[5m]) * 100

# File descriptors
process_open_fds{job=~"api-gateway|quorum-coordinator|storage-nodes"}
```

### Consistency Level Comparison

#### Latency by Consistency Level
```promql
# Write latency comparison (requires consistency label in metrics)
# Note: You may need to add custom metrics with consistency level labels
# For now, this is a template that would work if consistency is tracked:
# rate(http_server_requests_seconds_sum{job="api-gateway", consistency="ONE"}[5m]) / 
# rate(http_server_requests_seconds_count{job="api-gateway", consistency="ONE"}[5m]) * 1000
```

### Aggregated Metrics

#### Total System Throughput
```promql
# Total write operations across all nodes
sum(rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/write"}[5m]))

# Total read operations across all nodes
sum(rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/read/{key}"}[5m]))
```

#### Average Replication Factor
```promql
# Average number of nodes responding (approximation)
count(count by (instance) (rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])))
```

---

## Grafana Dashboard Queries

### Panel 1: Request Rate Over Time
**Query:**
```promql
rate(http_server_requests_seconds_count{job="api-gateway"}[5m])
```
**Legend:** `{{method}} {{uri}}`
**Unit:** requests/sec

### Panel 2: Write Latency (Average)
**Query:**
```promql
rate(http_server_requests_seconds_sum{job="api-gateway", uri="/api/logs", method="POST"}[5m]) / 
rate(http_server_requests_seconds_count{job="api-gateway", uri="/api/logs", method="POST"}[5m]) * 1000
```
**Legend:** Average Write Latency
**Unit:** milliseconds

### Panel 3: Read Latency (Average)
**Query:**
```promql
rate(http_server_requests_seconds_sum{job="api-gateway", uri=~"/api/logs/.*", method="GET"}[5m]) / 
rate(http_server_requests_seconds_count{job="api-gateway", uri=~"/api/logs/.*", method="GET"}[5m]) * 1000
```
**Legend:** Average Read Latency
**Unit:** milliseconds

### Panel 4: Latency Percentiles (Write)
**Queries:**
```promql
# P50
histogram_quantile(0.50, rate(http_server_requests_seconds_bucket{job="api-gateway", uri="/api/logs", method="POST"}[5m])) * 1000

# P95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="api-gateway", uri="/api/logs", method="POST"}[5m])) * 1000

# P99
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{job="api-gateway", uri="/api/logs", method="POST"}[5m])) * 1000
```
**Unit:** milliseconds

### Panel 5: Error Rate
**Query:**
```promql
rate(http_server_requests_seconds_count{job="api-gateway", status=~"4..|5.."}[5m])
```
**Legend:** `{{status}}`
**Unit:** errors/sec

### Panel 6: Success Rate Percentage
**Query:**
```promql
(rate(http_server_requests_seconds_count{job="api-gateway", status=~"2.."}[5m]) / 
 rate(http_server_requests_seconds_count{job="api-gateway"}[5m])) * 100
```
**Legend:** Success Rate
**Unit:** percent (0-100)

### Panel 7: Quorum Operations Rate
**Query:**
```promql
rate(http_server_requests_seconds_count{job="quorum-coordinator", uri=~"/quorum/(write|read)"}[5m])
```
**Legend:** `{{uri}}`
**Unit:** ops/sec

### Panel 8: Quorum Latency
**Query:**
```promql
rate(http_server_requests_seconds_sum{job="quorum-coordinator", uri=~"/quorum/(write|read)"}[5m]) / 
rate(http_server_requests_seconds_count{job="quorum-coordinator", uri=~"/quorum/(write|read)"}[5m]) * 1000
```
**Legend:** `{{uri}}`
**Unit:** milliseconds

### Panel 9: Quorum Success Rate
**Query:**
```promql
(rate(http_server_requests_seconds_count{job="quorum-coordinator", status="200"}[5m]) / 
 rate(http_server_requests_seconds_count{job="quorum-coordinator"}[5m])) * 100
```
**Legend:** Quorum Success Rate
**Unit:** percent

### Panel 10: Circuit Breaker State
**Query:**
```promql
resilience4j_circuitbreaker_state{job="quorum-coordinator", name="storageNode"}
```
**Legend:** Circuit Breaker State
**Unit:** state (0=closed, 1=open, 2=half-open)

### Panel 11: Storage Node Request Rate
**Query:**
```promql
rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])
```
**Legend:** `{{instance}}`
**Unit:** requests/sec

### Panel 12: Storage Node Latency
**Query:**
```promql
rate(http_server_requests_seconds_sum{job="storage-nodes"}[5m]) / 
rate(http_server_requests_seconds_count{job="storage-nodes"}[5m]) * 1000
```
**Legend:** `{{instance}}`
**Unit:** milliseconds

### Panel 13: Storage Node Health
**Query:**
```promql
(rate(http_server_requests_seconds_count{job="storage-nodes", status="200"}[5m]) / 
 rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])) * 100
```
**Legend:** `{{instance}}`
**Unit:** percent

### Panel 14: JVM Heap Memory Usage
**Query:**
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```
**Legend:** `{{job}}`
**Unit:** percent

### Panel 15: Total System Throughput
**Query:**
```promql
sum(rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/write"}[5m]))
```
**Legend:** Total Writes
**Unit:** ops/sec

**Query:**
```promql
sum(rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/read/{key}"}[5m]))
```
**Legend:** Total Reads
**Unit:** ops/sec

### Panel 16: Active Storage Nodes
**Query:**
```promql
count(count by (instance) (rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])))
```
**Legend:** Active Nodes
**Unit:** count

---

## Useful Alerts

### High Error Rate
```promql
(rate(http_server_requests_seconds_count{job="api-gateway", status=~"4..|5.."}[5m]) / 
 rate(http_server_requests_seconds_count{job="api-gateway"}[5m])) * 100 > 5
```

### High Latency
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="api-gateway"}[5m])) * 1000 > 1000
```

### Circuit Breaker Open
```promql
resilience4j_circuitbreaker_state{job="quorum-coordinator", name="storageNode"} == 1
```

### Low Quorum Success Rate
```promql
(rate(http_server_requests_seconds_count{job="quorum-coordinator", status="200"}[5m]) / 
 rate(http_server_requests_seconds_count{job="quorum-coordinator"}[5m])) * 100 < 90
```

### Storage Node Down
```promql
up{job="storage-nodes"} == 0
```

### High Memory Usage
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100 > 80
```

