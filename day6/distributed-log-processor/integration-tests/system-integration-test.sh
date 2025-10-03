#!/bin/bash

# Integration test script for the distributed log processing system

BASE_URL="http://localhost"
PRODUCER_PORT="8081"
GATEWAY_PORT="8080"

echo "Running System Integration Tests..."

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Test 1: Health checks
echo "Testing health endpoints..."
curl -f "$BASE_URL:$PRODUCER_PORT/actuator/health" || exit 1
curl -f "$BASE_URL:$GATEWAY_PORT/actuator/health" || exit 1

# Test 2: Send test log
echo "Sending test log event..."
RESPONSE=$(curl -s -X POST "$BASE_URL:$PRODUCER_PORT/api/v1/logs" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Integration test log message",
    "level": "INFO",
    "source": "integration-test",
    "metadata": {
      "test": "true",
      "environment": "local"
    }
  }')

if [[ $? -ne 0 ]]; then
  echo "Failed to send log event"
  exit 1
fi

echo "Log sent successfully"

# Wait for processing
sleep 5

# Test 3: Query logs
echo "Querying logs..."
QUERY_RESPONSE=$(curl -s "$BASE_URL:$GATEWAY_PORT/api/v1/logs?keyword=integration&size=10")

if [[ $? -ne 0 ]]; then
  echo "Failed to query logs"
  exit 1
fi

echo "Query successful"

# Test 4: Get statistics
echo "Getting log statistics..."
STATS_RESPONSE=$(curl -s "$BASE_URL:$GATEWAY_PORT/api/v1/logs/stats")

if [[ $? -ne 0 ]]; then
  echo "Failed to get statistics"
  exit 1
fi

echo "Statistics retrieved successfully"

echo "All integration tests passed!"
