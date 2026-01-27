#!/bin/bash

echo "🧪 Running integration tests..."

# Test dashboard health
echo "Testing dashboard service..."
DASHBOARD_HEALTH=$(curl -s http://localhost:8080/api/metrics/health)
if [ "$DASHBOARD_HEALTH" == "Dashboard API is healthy" ]; then
    echo "✅ Dashboard service is healthy"
else
    echo "❌ Dashboard service health check failed"
    exit 1
fi

# Test WebSocket endpoint
echo "Testing WebSocket endpoint..."
WEBSOCKET_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ws-metrics)
if [ "$WEBSOCKET_RESPONSE" == "200" ]; then
    echo "✅ WebSocket endpoint is accessible"
else
    echo "⚠️  WebSocket endpoint returned: $WEBSOCKET_RESPONSE"
fi

# Test Prometheus metrics
echo "Testing metrics endpoints..."
PROM_RESPONSE=$(curl -s http://localhost:8081/actuator/prometheus | grep "jvm_memory_used_bytes" | wc -l)
if [ "$PROM_RESPONSE" -gt "0" ]; then
    echo "✅ Prometheus metrics are being exported"
else
    echo "❌ Prometheus metrics check failed"
    exit 1
fi

echo ""
echo "✅ All integration tests passed!"
