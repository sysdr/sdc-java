#!/bin/bash

echo "🧪 Testing Alert Generation Flow"
echo "================================="

API_URL="http://localhost:8080"

# Test 1: Generate ERROR logs to trigger alert
echo -e "\n1️⃣ Generating ERROR logs to trigger threshold alert..."
for i in {1..120}; do
  curl -s -X POST ${API_URL}/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"service\": \"test-service\",
      \"level\": \"ERROR\",
      \"message\": \"Test error message $i\",
      \"statusCode\": 500
    }" > /dev/null
done
echo "✅ Sent 120 ERROR logs"

# Wait for processing
sleep 10

# Test 2: Generate high-latency logs
echo -e "\n2️⃣ Generating high-latency logs..."
for i in {1..60}; do
  curl -s -X POST ${API_URL}/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"service\": \"test-service\",
      \"level\": \"INFO\",
      \"message\": \"Slow request $i\",
      \"responseTime\": 3000
    }" > /dev/null
done
echo "✅ Sent 60 high-latency logs"

# Test 3: Generate 5xx errors
echo -e "\n3️⃣ Generating 5xx errors..."
for i in {1..30}; do
  curl -s -X POST ${API_URL}/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"service\": \"test-service\",
      \"level\": \"ERROR\",
      \"message\": \"Server error $i\",
      \"statusCode\": 503
    }" > /dev/null
done
echo "✅ Sent 30 5xx error logs"

echo -e "\n✅ Integration test complete!"
echo "Check alert-manager and notification-service logs for triggered alerts"
