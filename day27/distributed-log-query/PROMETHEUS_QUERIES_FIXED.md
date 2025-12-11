# Fixed Prometheus Queries

## Issue
The `query_execution_time_seconds` metric is currently a **Summary** type, not a **Histogram**. This means `histogram_quantile()` won't work until the service is restarted with the updated code.

## Solution Applied
I've updated the code to enable percentile histogram publishing. **You need to restart the query-coordinator service** for this to take effect.

## Current Queries (Work Now - Summary Metrics)

Since the metric is currently a Summary, use these queries:

### Average Query Latency
```promql
rate(query_execution_time_seconds_sum[5m]) / rate(query_execution_time_seconds_count[5m])
```

### Query Rate
```promql
rate(query_execution_time_seconds_count[5m])
```

### Max Query Latency
```promql
query_execution_time_seconds_max
```

### Total Queries
```promql
query_execution_time_seconds_count
```

## After Restart (Histogram Queries - Percentiles)

Once you restart the service, these percentile queries will work:

### P50 (Median) Query Latency
```promql
histogram_quantile(0.50, rate(query_execution_time_seconds_bucket[5m]))
```

### P95 Query Latency
```promql
histogram_quantile(0.95, rate(query_execution_time_seconds_bucket[5m]))
```

### P99 Query Latency
```promql
histogram_quantile(0.99, rate(query_execution_time_seconds_bucket[5m]))
```

### P99.9 Query Latency
```promql
histogram_quantile(0.999, rate(query_execution_time_seconds_bucket[5m]))
```

## How to Restart the Service

### Option 1: Restart via Docker Compose
```bash
cd /home/systemdr/git/sdc-java/day27/distributed-log-query
docker-compose restart query-coordinator
```

### Option 2: Rebuild and Restart
```bash
cd /home/systemdr/git/sdc-java/day27/distributed-log-query
docker-compose stop query-coordinator
docker-compose build query-coordinator
docker-compose up -d query-coordinator
```

### Option 3: Full System Restart
```bash
cd /home/systemdr/git/sdc-java/day27/distributed-log-query
docker-compose down
docker-compose up -d
```

## Verify the Fix

After restarting, verify that histogram buckets are available:

```bash
curl -s http://localhost:8080/actuator/prometheus | grep "query_execution_time_seconds_bucket"
```

You should see multiple bucket entries like:
```
query_execution_time_seconds_bucket{le="0.001"} 0.0
query_execution_time_seconds_bucket{le="0.005"} 0.0
query_execution_time_seconds_bucket{le="0.01"} 0.0
...
```

## Why This Happened

Micrometer's `Timer` by default creates a **Summary** metric which only tracks:
- Count
- Sum
- Max

To get percentiles (P50, P95, P99), you need to enable `publishPercentileHistogram(true)`, which creates histogram buckets that Prometheus can use with `histogram_quantile()`.

## Quick Test

After restart, run some queries and then test:

```bash
# Generate some metrics
./load-test.sh

# Wait a few seconds for Prometheus to scrape
sleep 10

# Test the P95 query in Prometheus
curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95, rate(query_execution_time_seconds_bucket[5m]))" | jq .
```

