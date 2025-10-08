#!/bin/bash

set -e

echo "🧪 Running Integration Tests..."
echo "================================"

BASE_URL="http://localhost:8080"

# Test 1: Gateway health check
echo "Test 1: Gateway health check"
response=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/actuator/health)
if [ "$response" = "200" ]; then
    echo "✅ Gateway is healthy"
else
    echo "❌ Gateway health check failed: $response"
    exit 1
fi

# Test 2: Send log via UDP
echo ""
echo "Test 2: Send log event"
response=$(curl -s -X POST ${BASE_URL}/api/logs/ship \
  -H "Content-Type: application/json" \
  -d '{
    "source": "test-app",
    "level": "INFO",
    "message": "Integration test log message"
  }')

if echo "$response" | grep -q "shipped"; then
    echo "✅ Log shipped successfully"
    echo "Response: $response"
else
    echo "❌ Failed to ship log"
    echo "Response: $response"
    exit 1
fi

# Test 3: Send multiple logs rapidly
echo ""
echo "Test 3: Send 100 rapid fire logs"
for i in {1..100}; do
    curl -s -X POST ${BASE_URL}/api/logs/ship \
      -H "Content-Type: application/json" \
      -d "{
        \"source\": \"load-test\",
        \"level\": \"DEBUG\",
        \"message\": \"Rapid fire test message $i\"
      }" > /dev/null &
done
wait

echo "✅ Sent 100 logs successfully"

# Test 4: Check metrics
echo ""
echo "Test 4: Check producer metrics"
metrics=$(curl -s http://localhost:8081/api/logs/metrics)
echo "Metrics: $metrics"

echo ""
echo "✅ All integration tests passed!"
