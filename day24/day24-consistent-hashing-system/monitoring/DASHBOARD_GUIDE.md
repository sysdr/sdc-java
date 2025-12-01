# Consistent Hashing System - Dashboard Guide

## Accessing the Dashboard

1. **Grafana**: http://localhost:3000
   - Username: `admin`
   - Password: `admin`

2. **Dashboard URL**: http://localhost:3000/d/consistent-hashing-dashboard

3. **Prometheus**: http://localhost:9090

## Dashboard Overview

The dashboard provides real-time monitoring of the Consistent Hashing System with the following panels:

### Key Metrics (Top Row)

1. **Total Logs Ingested (per minute)**
   - Shows the ingestion rate in logs per minute
   - Color coding: Green (normal) → Yellow → Orange → Red (high load)

2. **Active Storage Nodes**
   - Displays the number of active nodes in the hash ring
   - Color coding: Red (0) → Orange (1) → Yellow (2) → Green (3 nodes)

3. **Distribution Balance Score**
   - Percentage indicating how evenly logs are distributed
   - Higher is better (95%+ is excellent)
   - Color coding: Red (<70%) → Orange (70-85%) → Yellow (85-95%) → Green (95%+)

4. **Total Logs Stored**
   - Cumulative count of all logs stored across all nodes

### Detailed Visualizations

5. **Log Distribution Across Nodes**
   - Time series graph showing log count per node over time
   - Colors: Green (node-1), Orange (node-2), Red (node-3)
   - Helps identify if distribution is balanced

6. **Log Ingestion Rate**
   - Bar chart showing logs ingested per second
   - Teal color for easy visibility

7. **Logs Routed per Node**
   - Shows routing throughput per node
   - Helps identify which nodes are receiving traffic

8. **Current Log Count per Node (Bar Gauge)**
   - Horizontal bar chart showing current state
   - Color-coded by node with thresholds

9. **Distribution Balance Score Over Time**
   - Line graph tracking balance score
   - Teal color, shows trend over time

### Error Monitoring

10. **Dropped Logs (No Nodes Available)**
    - Red bars showing when logs are dropped
    - Should be 0 in healthy system

11. **Failed Routing Attempts**
    - Orange bars showing routing failures
    - Indicates node connectivity issues

## Prometheus Queries (Demo)

You can test these queries in Prometheus at http://localhost:9090:

### Basic Metrics
```
# Total logs ingested
sum(rate(logs_ingested_total[5m])) * 60

# Active nodes
count(storage_node_log_count)

# Balance score
storage_distribution_balance_score

# Total logs stored
sum(storage_node_log_count)
```

### Per-Node Metrics
```
# Logs per node
storage_node_log_count

# Routing rate per node
sum(rate(logs_routed_total[1m])) by (target_node)

# Ingestion rate
rate(logs_ingested_total[1m])
```

### Error Metrics
```
# Dropped logs
rate(logs_dropped_no_nodes_total[1m])

# Failed routing
rate(logs_routing_failed_total[1m])
```

## Understanding the System

### Consistent Hashing
- Logs are distributed across nodes using hash-based routing
- Each node has 150 virtual nodes for better distribution
- Adding/removing nodes affects only ~1/N of keys

### Color Scheme
The dashboard uses a color scheme that avoids purple/blue:
- **Green**: Healthy, good performance
- **Orange**: Warning, moderate load
- **Red**: Critical, high load or errors
- **Yellow**: Caution, approaching limits
- **Teal**: Informational metrics

### Key Indicators

**Healthy System:**
- Balance Score: 95%+
- All 3 nodes active
- No dropped logs
- Even distribution across nodes

**Issues to Watch:**
- Balance Score dropping below 85%
- Nodes going offline (count < 3)
- Dropped logs increasing
- Uneven distribution (one node much higher than others)

## Testing the Dashboard

1. Run load test to generate data:
   ```bash
   ./load-test.sh
   ```

2. Watch the dashboard update in real-time (refreshes every 10 seconds)

3. Check distribution metrics:
   ```bash
   curl http://localhost:8081/api/coordinator/metrics/distribution | jq
   ```

## Troubleshooting

If dashboard shows no data:
1. Verify services are running: `docker-compose ps`
2. Check Prometheus targets: http://localhost:9090/targets
3. Verify metrics endpoint: `curl http://localhost:8080/actuator/prometheus | head -20`
4. Wait 30-60 seconds for initial data collection

