#!/bin/bash

echo "📊 Dashboard Validation Report"
echo "=============================="
echo ""

# Check Prometheus
echo "1. Prometheus Status:"
PROM_STATUS=$(curl -s http://localhost:9090/-/healthy 2>&1)
if [ "$PROM_STATUS" = "Prometheus is Healthy." ]; then
    echo "   ✅ Prometheus is healthy"
else
    echo "   ⚠️  Prometheus status: $PROM_STATUS"
fi

# Check Grafana
echo ""
echo "2. Grafana Status:"
GRAFANA_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/health 2>&1)
if [ "$GRAFANA_STATUS" = "200" ]; then
    echo "   ✅ Grafana is accessible"
else
    echo "   ⚠️  Grafana status code: $GRAFANA_STATUS"
fi

# Check Metrics
echo ""
echo "3. Metrics Validation:"
echo "   Checking logs_ingested_total..."
LOGS_METRIC=$(curl -s 'http://localhost:9090/api/v1/query?query=logs_ingested_total' 2>&1)
if echo "$LOGS_METRIC" | grep -q '"value"'; then
    ERROR_COUNT=$(echo "$LOGS_METRIC" | grep -o '"ERROR"[^}]*"value":\[[^,]*,"[^"]*"' | grep -o '"[0-9]*"' | tail -1 | tr -d '"')
    INFO_COUNT=$(echo "$LOGS_METRIC" | grep -o '"INFO"[^}]*"value":\[[^,]*,"[^"]*"' | grep -o '"[0-9]*"' | tail -1 | tr -d '"')
    if [ -n "$ERROR_COUNT" ] && [ "$ERROR_COUNT" != "0" ]; then
        echo "   ✅ ERROR logs ingested: $ERROR_COUNT (non-zero ✓)"
    else
        echo "   ❌ ERROR logs: 0 or not found"
    fi
    if [ -n "$INFO_COUNT" ] && [ "$INFO_COUNT" != "0" ]; then
        echo "   ✅ INFO logs ingested: $INFO_COUNT (non-zero ✓)"
    else
        echo "   ❌ INFO logs: 0 or not found"
    fi
else
    echo "   ⚠️  Could not retrieve metrics"
fi

# Check Service Health
echo ""
echo "4. Service Health Status:"
for port in 8080 8081 8082 8083; do
    HEALTH=$(curl -s http://localhost:$port/health 2>&1)
    if echo "$HEALTH" | grep -q "UP"; then
        echo "   ✅ Port $port: UP"
    else
        echo "   ❌ Port $port: DOWN or no /health endpoint"
    fi
done

# Check for duplicate services
echo ""
echo "5. Duplicate Service Check:"
DUPLICATES=$(docker ps --filter "name=alert" --format "{{.Names}}" | sort | uniq -d)
if [ -z "$DUPLICATES" ]; then
    echo "   ✅ No duplicate services found"
else
    echo "   ⚠️  Duplicate services detected: $DUPLICATES"
fi

# Summary
echo ""
echo "=============================="
echo "📈 Dashboard Access:"
echo "   Prometheus: http://localhost:9090"
echo "   Grafana:    http://localhost:3000 (admin/admin)"
echo ""
echo "📊 Key Metrics to Check in Grafana:"
echo "   - logs_ingested_total (should be > 0)"
echo "   - Rate: rate(logs_ingested_total[5m])"
echo ""
echo "✅ Validation Complete!"
