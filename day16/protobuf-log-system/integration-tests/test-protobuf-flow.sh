#!/bin/bash

echo "Running Integration Tests..."

# Test JSON endpoint
echo "Test 1: JSON Endpoint"
RESPONSE=$(curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "test-001",
    "timestamp": "2024-01-01T10:00:00Z",
    "level": "INFO",
    "message": "Integration test message",
    "serviceName": "test-service"
  }' -s)

if echo "$RESPONSE" | grep -q "accepted"; then
  echo "✓ JSON endpoint working"
else
  echo "✗ JSON endpoint failed"
  exit 1
fi

sleep 2

# Test query endpoint
echo "Test 2: Query Recent Logs"
RESPONSE=$(curl -s http://localhost:8080/api/query/recent?limit=5)

if echo "$RESPONSE" | grep -q "test-001"; then
  echo "✓ Log stored and queryable"
else
  echo "✗ Log not found in storage"
  exit 1
fi

# Test metrics
echo "Test 3: Prometheus Metrics"
METRICS=$(curl -s http://localhost:8081/actuator/prometheus)

if echo "$METRICS" | grep -q "log_events_json_total"; then
  echo "✓ Metrics exposed correctly"
else
  echo "✗ Metrics not available"
  exit 1
fi

echo ""
echo "All integration tests passed!"
