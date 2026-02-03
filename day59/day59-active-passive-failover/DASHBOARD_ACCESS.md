# Dashboard Access Information

## 🎯 Quick Access URLs

### Grafana Dashboard
- **URL**: http://localhost:3000
- **Username**: `admin`
- **Password**: `admin`
- **Dashboard**: Active-Passive Failover System Dashboard (auto-loaded)

### Prometheus
- **URL**: http://localhost:9090
- **Query Interface**: http://localhost:9090/graph
- **Status**: http://localhost:9090/-/healthy
- **Targets**: http://localhost:9090/targets

### API Endpoints

#### API Gateway
- **Health Check**: http://localhost:8080/api/logs/health
- **System Status**: http://localhost:8080/api/system/status
- **Publish Log**: `POST http://localhost:8080/api/logs`
- **Metrics**: http://localhost:8080/actuator/prometheus
- **Health**: http://localhost:8080/actuator/health

#### Log Consumer 1 (Port 8081)
- **Failover Status**: http://localhost:8081/api/failover/status
- **Metrics**: http://localhost:8081/actuator/prometheus
- **Health**: http://localhost:8081/actuator/health

#### Log Consumer 2 (Port 8082)
- **Failover Status**: http://localhost:8082/api/failover/status
- **Metrics**: http://localhost:8082/actuator/prometheus
- **Health**: http://localhost:8082/actuator/health

---

## 📊 Grafana Dashboard Setup

### First Time Access

1. **Login to Grafana**:
   - Navigate to http://localhost:3000
   - Username: `admin`
   - Password: `admin`
   - You'll be prompted to change the password (optional)

2. **Add Prometheus Data Source** (if not auto-configured):
   - Go to: Configuration → Data Sources → Add data source
   - Select: Prometheus
   - URL: `http://prometheus:9090` (from within Docker network)
   - Or: `http://host.docker.internal:9090` (from host)
   - Click "Save & Test"

3. **Import Dashboard**:
   - Go to: Dashboards → Import
   - Upload: `monitoring/dashboards/failover-dashboard.json`
   - Or copy the JSON content from the file
   - Select Prometheus as data source
   - Click "Import"

### Dashboard Panels

The dashboard includes the following panels:

1. **Messages Processed Total** - Total count of messages processed
2. **Leader Status** - Current leader/standby status (1=Leader, 0=Standby)
3. **Current Epoch** - Current leadership epoch number
4. **Failover Events Total** - Total number of failover events
5. **Messages Processed Rate** - Rate of message processing (messages/sec)
6. **Recovery Time (P95)** - 95th percentile recovery time after failover
7. **Leader Status Over Time** - Timeline showing leader transitions
8. **Total Messages Processed** - Cumulative message count over time
9. **Failover Events Timeline** - Timeline of failover events

---

## 🔍 Prometheus Queries

### Key Metrics Queries

```promql
# Total messages processed
messages_processed_total

# Messages per second
rate(messages_processed_total[1m])

# Leader status (1=leader, 0=standby)
leader_status

# Current epoch
leader_epoch

# Total failover events
failover_events_total

# Recovery time (P95)
histogram_quantile(0.95, failover_recovery_time_seconds)

# Recovery time (P99)
histogram_quantile(0.99, failover_recovery_time_seconds)

# Failover rate
rate(failover_events_total[5m])
```

### Example Queries in Prometheus UI

1. Go to http://localhost:9090/graph
2. Enter query: `messages_processed_total`
3. Click "Execute"
4. View results in table or graph format

---

## 🧪 Testing Dashboard Updates

### Generate Test Data

Run the demo script to generate messages and update metrics:

```bash
cd /home/systemdr03/git/sdc-java/day59/day59-active-passive-failover
./publish-demo-messages.sh
```

Or manually publish messages:

```bash
for i in {1..50}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"level\":\"INFO\",\"message\":\"Test message $i\",\"source\":\"test\"}"
  sleep 0.1
done
```

### Verify Metrics Update

1. **Check Prometheus**:
   ```bash
   curl http://localhost:9090/api/v1/query?query=messages_processed_total
   ```

2. **Check Consumer Status**:
   ```bash
   curl http://localhost:8081/api/failover/status | jq '.messagesProcessed'
   ```

3. **Check Grafana Dashboard**:
   - Refresh the dashboard (F5 or click refresh icon)
   - Values should update within 5-10 seconds

---

## 🔧 Troubleshooting

### Grafana Not Showing Data

1. **Check Prometheus Data Source**:
   - Go to Configuration → Data Sources
   - Click "Test" on Prometheus data source
   - Should show "Data source is working"

2. **Check Prometheus Targets**:
   - Go to http://localhost:9090/targets
   - Both `api-gateway` and `log-consumer` should be "UP"

3. **Check Metrics Endpoint**:
   ```bash
   curl http://localhost:8081/actuator/prometheus | grep messages_processed_total
   ```

### Prometheus Not Scraping

1. **Check Prometheus Config**:
   ```bash
   docker exec day59-active-passive-failover-prometheus-1 cat /etc/prometheus/prometheus.yml
   ```

2. **Check Service Accessibility**:
   - From Prometheus container: `host.docker.internal:8081` should be reachable
   - If not, check Docker network configuration

3. **Restart Prometheus**:
   ```bash
   docker restart day59-active-passive-failover-prometheus-1
   ```

### Metrics Showing Zero

1. **Verify Services Are Running**:
   ```bash
   ps aux | grep -E "(api-gateway|log-consumer)"
   ```

2. **Check Service Logs**:
   ```bash
   tail -f /tmp/api-gateway.log
   tail -f /tmp/log-consumer-1.log
   ```

3. **Publish Test Messages**:
   ```bash
   ./publish-demo-messages.sh
   ```

4. **Wait for Scrape Interval**:
   - Prometheus scrapes every 5 seconds
   - Wait 5-10 seconds after publishing messages

---

## 📈 Expected Dashboard Values

After running the demo, you should see:

- ✅ **messages_processed_total**: > 0 (e.g., 18, 30, 50+)
- ✅ **leader_status**: 1.0 (if leader) or 0.0 (if standby)
- ✅ **leader_epoch**: >= 1
- ✅ **failover_events_total**: >= 1 (at least one leader election)
- ✅ **Recovery Time**: Should show values if failover occurred

All values should be **non-zero** and **updating** when messages are processed.

---

## 🔗 Quick Links Summary

| Service | URL | Purpose |
|---------|-----|---------|
| Grafana | http://localhost:3000 | Main dashboard (admin/admin) |
| Prometheus | http://localhost:9090 | Metrics query interface |
| API Gateway | http://localhost:8080 | REST API for publishing logs |
| Consumer 1 | http://localhost:8081 | First consumer instance |
| Consumer 2 | http://localhost:8082 | Second consumer instance |

---

## 📝 Notes

- **Refresh Rate**: Grafana dashboard refreshes every 5 seconds
- **Scrape Interval**: Prometheus scrapes metrics every 5 seconds
- **Data Retention**: Prometheus data is stored in Docker volume
- **Dashboard Auto-Load**: The dashboard JSON is in `monitoring/dashboards/` and should auto-load if Grafana is configured correctly

For more information, see the main README.md file.
