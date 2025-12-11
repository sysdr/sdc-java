# Prometheus & Grafana Query Guide

This guide provides useful queries for monitoring the Distributed Log Query System.

## Access Points
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

---

## Prometheus Queries

### 1. Query Execution Time (Latency Metrics)

#### Average Query Latency
```promql
rate(query_execution_time_seconds_sum[5m]) / rate(query_execution_time_seconds_count[5m])
```

#### P50 (Median) Query Latency
```promql
histogram_quantile(0.50, rate(query_execution_time_seconds_bucket[5m]))
```

#### P95 Query Latency
```promql
histogram_quantile(0.95, rate(query_execution_time_seconds_bucket[5m]))
```

#### P99 Query Latency
```promql
histogram_quantile(0.99, rate(query_execution_time_seconds_bucket[5m]))
```

#### Max Query Latency
```promql
histogram_quantile(1.0, rate(query_execution_time_seconds_bucket[5m]))
```

### 2. Query Throughput

#### Query Rate (queries per second)
```promql
rate(query_execution_time_seconds_count[5m])
```

#### Total Queries Executed
```promql
query_execution_time_seconds_count
```

### 3. Partition Metrics

#### Partitions Queried Rate
```promql
rate(query_partitions_queried_total[5m])
```

#### Partitions Pruned Rate
```promql
rate(query_partitions_pruned_total[5m])
```

#### Total Partitions Queried
```promql
query_partitions_queried_total
```

#### Total Partitions Pruned
```promql
query_partitions_pruned_total
```

#### Partition Pruning Effectiveness (Percentage)
```promql
(rate(query_partitions_pruned_total[5m]) / (rate(query_partitions_queried_total[5m]) + rate(query_partitions_pruned_total[5m]))) * 100
```

#### Average Partitions Queried Per Query
```promql
rate(query_partitions_queried_total[5m]) / rate(query_execution_time_seconds_count[5m])
```

### 4. Spring Boot Actuator Metrics

#### HTTP Request Rate
```promql
rate(http_server_requests_seconds_count[5m])
```

#### HTTP Request Latency (P95)
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

#### JVM Memory Used
```promql
jvm_memory_used_bytes{area="heap"}
```

#### JVM Memory Max
```promql
jvm_memory_max_bytes{area="heap"}
```

#### JVM Memory Usage Percentage
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

#### JVM GC Pause Time
```promql
rate(jvm_gc_pause_seconds_sum[5m])
```

#### Process CPU Usage
```promql
process_cpu_usage
```

#### System CPU Usage
```promql
system_cpu_usage
```

### 5. Service Health

#### All Available Metrics
```promql
{__name__=~".+"}
```

#### List All Custom Query Metrics
```promql
{__name__=~"query_.+"}
```

---

## Grafana Dashboard Queries

### Panel 1: Query Latency Over Time (Line Graph)
**Title**: Query Execution Latency (P50, P95, P99)

**Queries**:
```promql
# P50
histogram_quantile(0.50, rate(query_execution_time_seconds_bucket[5m])) 
# Legend: P50

# P95
histogram_quantile(0.95, rate(query_execution_time_seconds_bucket[5m])) 
# Legend: P95

# P99
histogram_quantile(0.99, rate(query_execution_time_seconds_bucket[5m])) 
# Legend: P99
```

**Visualization**: Time series, Y-axis: seconds

---

### Panel 2: Query Throughput (Stat Panel)
**Title**: Queries Per Second

**Query**:
```promql
rate(query_execution_time_seconds_count[5m])
```

**Visualization**: Stat panel, Unit: ops/sec

---

### Panel 3: Partition Pruning Effectiveness (Gauge)
**Title**: Partition Pruning Rate

**Query**:
```promql
(rate(query_partitions_pruned_total[5m]) / (rate(query_partitions_queried_total[5m]) + rate(query_partitions_pruned_total[5m]))) * 100
```

**Visualization**: Gauge, Min: 0, Max: 100, Unit: percent

---

### Panel 4: Partitions Queried vs Pruned (Bar Chart)
**Title**: Partition Activity

**Queries**:
```promql
# Queried
rate(query_partitions_queried_total[5m])
# Legend: Queried

# Pruned
rate(query_partitions_pruned_total[5m])
# Legend: Pruned
```

**Visualization**: Bar chart, Y-axis: partitions/sec

---

### Panel 5: Total Queries (Stat Panel)
**Title**: Total Queries Executed

**Query**:
```promql
query_execution_time_seconds_count
```

**Visualization**: Stat panel, Unit: none

---

### Panel 6: Average Partitions Per Query (Stat Panel)
**Title**: Avg Partitions Per Query

**Query**:
```promql
rate(query_partitions_queried_total[5m]) / rate(query_execution_time_seconds_count[5m])
```

**Visualization**: Stat panel, Unit: partitions

---

### Panel 7: JVM Memory Usage (Graph)
**Title**: JVM Heap Memory

**Queries**:
```promql
# Used
jvm_memory_used_bytes{area="heap"}
# Legend: Used

# Max
jvm_memory_max_bytes{area="heap"}
# Legend: Max
```

**Visualization**: Time series, Y-axis: bytes

---

### Panel 8: HTTP Request Rate (Graph)
**Title**: HTTP Requests Per Second

**Query**:
```promql
rate(http_server_requests_seconds_count[5m])
```

**Visualization**: Time series, Y-axis: req/sec

---

### Panel 9: System CPU Usage (Graph)
**Title**: CPU Usage

**Query**:
```promql
system_cpu_usage * 100
```

**Visualization**: Time series, Y-axis: percent (0-100)

---

### Panel 10: Query Success Rate (if error metrics available)
**Title**: Query Success Rate

**Query**:
```promql
rate(query_execution_time_seconds_count[5m]) / (rate(query_execution_time_seconds_count[5m]) + rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

**Visualization**: Stat panel, Unit: percent

---

## Quick Reference: Key Metrics

| Metric Name | Type | Description |
|------------|------|-------------|
| `query_execution_time_seconds` | Histogram | Query execution time distribution |
| `query_partitions_queried_total` | Counter | Total partitions queried |
| `query_partitions_pruned_total` | Counter | Total partitions pruned |
| `http_server_requests_seconds` | Histogram | HTTP request latency (Spring Boot) |
| `jvm_memory_used_bytes` | Gauge | JVM memory usage |
| `jvm_gc_pause_seconds` | Timer | GC pause time |
| `process_cpu_usage` | Gauge | Process CPU usage |
| `system_cpu_usage` | Gauge | System CPU usage |

---

## Tips

1. **Time Ranges**: Adjust `[5m]` to your preferred time window (e.g., `[1m]`, `[15m]`, `[1h]`)
2. **Aggregation**: Use `sum()` or `avg()` if you have multiple instances
3. **Filtering**: Add label filters like `{job="query-coordinator"}` to filter by service
4. **Alerts**: Set up alerts for:
   - P99 latency > 1 second
   - Query rate drops to 0
   - Memory usage > 80%
   - Partition pruning effectiveness < 50%

---

## Example Alert Rules

### High Query Latency Alert
```yaml
- alert: HighQueryLatency
  expr: histogram_quantile(0.99, rate(query_execution_time_seconds_bucket[5m])) > 1
  for: 5m
  annotations:
    summary: "Query latency is high"
    description: "P99 query latency is {{ $value }}s"
```

### Low Partition Pruning Alert
```yaml
- alert: LowPartitionPruning
  expr: (rate(query_partitions_pruned_total[5m]) / (rate(query_partitions_queried_total[5m]) + rate(query_partitions_pruned_total[5m]))) < 0.5
  for: 10m
  annotations:
    summary: "Partition pruning effectiveness is low"
    description: "Only {{ $value }}% of partitions are being pruned"
```

