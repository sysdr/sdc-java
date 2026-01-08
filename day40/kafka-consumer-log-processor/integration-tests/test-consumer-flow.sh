#!/bin/bash

echo "🧪 Testing Kafka Consumer Flow..."

# Test 1: Send log via gateway
echo "Test 1: Sending log event..."
RESPONSE=$(curl -s -X POST http://localhost:8083/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "message": "Test log message",
    "service": "test-service",
    "host": "localhost",
    "userId": "test-user-123",
    "environment": "development",
    "version": "1.0.0"
  }')

echo "Response: $RESPONSE"

# Wait for processing
sleep 5

# Test 2: Check consumer metrics
echo "Test 2: Checking consumer metrics..."
curl -s http://localhost:8082/actuator/metrics/log.consumer.success | jq .

# Test 3: Verify Redis aggregations
echo "Test 3: Checking Redis aggregations..."
docker exec $(docker ps --format "{{.Names}}" | grep redis | head -1) redis-cli KEYS "logs:*" 2>/dev/null || echo "Redis keys check skipped"

# Test 4: Check Prometheus metrics
echo "Test 4: Querying Prometheus..."
curl -s 'http://localhost:9090/api/v1/query?query=log_consumer_success_total' | jq .

echo "✅ Integration tests completed!"
