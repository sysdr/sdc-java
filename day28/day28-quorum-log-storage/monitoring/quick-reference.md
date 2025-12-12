# Quick Reference: Prometheus & Grafana Queries

## Most Common Prometheus Queries

### Request Rate
```promql
rate(http_server_requests_seconds_count{job="api-gateway"}[5m])
```

### Average Latency (ms)
```promql
rate(http_server_requests_seconds_sum{job="api-gateway"}[5m]) / 
rate(http_server_requests_seconds_count{job="api-gateway"}[5m]) * 1000
```

### 95th Percentile Latency (ms)
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="api-gateway"}[5m])) * 1000
```

### Error Rate
```promql
rate(http_server_requests_seconds_count{job="api-gateway", status=~"4..|5.."}[5m])
```

### Quorum Write Latency
```promql
rate(http_server_requests_seconds_sum{job="quorum-coordinator", uri="/quorum/write"}[5m]) / 
rate(http_server_requests_seconds_count{job="quorum-coordinator", uri="/quorum/write"}[5m]) * 1000
```

### Quorum Success Rate (%)
```promql
(rate(http_server_requests_seconds_count{job="quorum-coordinator", status="200"}[5m]) / 
 rate(http_server_requests_seconds_count{job="quorum-coordinator"}[5m])) * 100
```

### Circuit Breaker State
```promql
resilience4j_circuitbreaker_state{job="quorum-coordinator", name="storageNode"}
```

### Storage Node Request Rate
```promql
rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])
```

### Storage Node Latency
```promql
rate(http_server_requests_seconds_sum{job="storage-nodes"}[5m]) / 
rate(http_server_requests_seconds_count{job="storage-nodes"}[5m]) * 1000
```

### JVM Heap Memory Usage (%)
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

### Total System Throughput
```promql
sum(rate(http_server_requests_seconds_count{job="storage-nodes", uri="/storage/write"}[5m]))
```

---

## Grafana Panel Configurations

### Time Series Panel: Request Rate
- **Query:** `rate(http_server_requests_seconds_count{job="api-gateway"}[5m])`
- **Legend:** `{{method}} {{uri}}`
- **Unit:** requests/sec
- **Y-axis:** Linear

### Time Series Panel: Latency (P50, P95, P99)
- **Queries:**
  - P50: `histogram_quantile(0.50, rate(http_server_requests_seconds_bucket{job="api-gateway"}[5m])) * 1000`
  - P95: `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="api-gateway"}[5m])) * 1000`
  - P99: `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{job="api-gateway"}[5m])) * 1000`
- **Unit:** milliseconds
- **Y-axis:** Linear

### Stat Panel: Success Rate
- **Query:** `(rate(http_server_requests_seconds_count{job="api-gateway", status=~"2.."}[5m]) / rate(http_server_requests_seconds_count{job="api-gateway"}[5m])) * 100`
- **Unit:** percent (0-100)
- **Thresholds:** Green: >95, Yellow: 90-95, Red: <90

### Gauge Panel: Circuit Breaker State
- **Query:** `resilience4j_circuitbreaker_state{job="quorum-coordinator", name="storageNode"}`
- **Unit:** state
- **Thresholds:** Green: 0 (closed), Yellow: 2 (half-open), Red: 1 (open)

### Bar Gauge: Storage Node Health
- **Query:** `(rate(http_server_requests_seconds_count{job="storage-nodes", status="200"}[5m]) / rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])) * 100`
- **Legend:** `{{instance}}`
- **Unit:** percent
- **Orientation:** Horizontal

### Table Panel: Node Status
- **Queries:**
  - Node: `label_replace(up{job="storage-nodes"}, "node", "$1", "instance", "(.*)")`
  - Requests/sec: `rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])`
  - Avg Latency: `rate(http_server_requests_seconds_sum{job="storage-nodes"}[5m]) / rate(http_server_requests_seconds_count{job="storage-nodes"}[5m]) * 1000`
  - Success Rate: `(rate(http_server_requests_seconds_count{job="storage-nodes", status="200"}[5m]) / rate(http_server_requests_seconds_count{job="storage-nodes"}[5m])) * 100`

---

## Quick Test Queries

Copy these into Prometheus (http://localhost:9090) to test:

1. **Check if metrics are being scraped:**
   ```promql
   up{job=~"api-gateway|quorum-coordinator|storage-nodes"}
   ```

2. **List all available metrics:**
   ```promql
   {__name__=~".+"}
   ```

3. **Check HTTP endpoints:**
   ```promql
   http_server_requests_seconds_count
   ```

4. **Check circuit breaker metrics:**
   ```promql
   resilience4j_circuitbreaker_state
   ```

