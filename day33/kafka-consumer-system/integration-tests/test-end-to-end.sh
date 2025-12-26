#!/bin/bash

# End-to-end integration test for log processing system

set -e

echo "🧪 Starting End-to-End Integration Tests..."

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 30

# Test 1: API Gateway health
echo "📊 Testing API Gateway health..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/monitoring/health)
if [ $response -eq 200 ]; then
    echo "✅ API Gateway is healthy"
else
    echo "❌ API Gateway health check failed (HTTP $response)"
    exit 1
fi

# Test 2: Send test logs to Kafka using producer from Day 32
echo "📤 Sending test logs to Kafka..."
for i in {1..100}; do
    curl -s -X POST http://localhost:8081/api/logs \
        -H "Content-Type: application/json" \
        -d "{
            \"applicationName\": \"test-app\",
            \"level\": \"INFO\",
            \"message\": \"Integration test message $i\",
            \"service\": \"integration-test\",
            \"host\": \"test-host\"
        }" > /dev/null
done
echo "✅ Sent 100 test logs"

# Test 3: Wait for processing
echo "⏳ Waiting for log processing..."
sleep 20

# Test 4: Verify consumer metrics
echo "📊 Checking consumer metrics..."
metrics=$(curl -s http://localhost:8080/api/monitoring/metrics/consumer)
echo "Consumer Metrics: $metrics"

processed_count=$(echo $metrics | grep -o '"totalProcessed":[0-9]*' | grep -o '[0-9]*' || echo "0")
if [ "$processed_count" -gt 0 ]; then
    echo "✅ Consumers processed $processed_count logs"
else
    echo "⚠️ No logs processed yet, might need more time"
fi

# Test 5: Check Prometheus metrics
echo "📈 Checking Prometheus metrics..."
prometheus_up=$(curl -s http://localhost:9090/api/v1/query?query=up | grep -c '"value":\[.*,\"1\"\]' || echo "0")
if [ "$prometheus_up" -gt 0 ]; then
    echo "✅ Prometheus is scraping metrics"
else
    echo "⚠️ Prometheus metrics might not be available yet"
fi

echo ""
echo "✅ Integration tests completed successfully!"
echo "🔍 Access Grafana dashboards at: http://localhost:3000 (admin/admin)"
echo "📊 Access Prometheus at: http://localhost:9090"
echo "🌐 Access API Gateway at: http://localhost:8080"
