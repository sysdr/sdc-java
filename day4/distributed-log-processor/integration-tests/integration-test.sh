#!/bin/bash

echo "🧪 Running integration tests..."

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Test 1: Producer health check
echo "🔍 Testing log producer health..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/logs/health)
if [ "$response" -eq 200 ]; then
    echo "✅ Log producer is healthy"
else
    echo "❌ Log producer health check failed (HTTP $response)"
    exit 1
fi

# Test 2: Parser health check
echo "🔍 Testing log parser health..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health)
if [ "$response" -eq 200 ]; then
    echo "✅ Log parser is healthy"
else
    echo "❌ Log parser health check failed (HTTP $response)"
    exit 1
fi

# Test 3: API Gateway health check
echo "🔍 Testing API gateway health..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/logs/health)
if [ "$response" -eq 200 ]; then
    echo "✅ API gateway is healthy"
else
    echo "❌ API gateway health check failed (HTTP $response)"
    exit 1
fi

# Test 4: Send a test log and verify processing
echo "🔍 Testing end-to-end log processing..."
test_log='192.168.1.100 - - [27/Sep/2025:10:30:00 +0000] "GET /test HTTP/1.1" 200 1234'

# Send test log
curl -s -X POST "http://localhost:8081/api/logs/send" \
    -H "Content-Type: text/plain" \
    -d "$test_log" > /dev/null

# Wait for processing
sleep 5

# Check if log appears in API
response=$(curl -s "http://localhost:8080/api/logs?size=1")
if echo "$response" | grep -q "192.168.1.100"; then
    echo "✅ End-to-end processing works"
else
    echo "❌ End-to-end processing failed"
    echo "Response: $response"
    exit 1
fi

echo ""
echo "🎉 All integration tests passed!"
