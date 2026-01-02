#!/bin/bash

echo "Testing Log Routing System..."

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Test 1: Send security log
echo "Test 1: Sending security log..."
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "severity": "ERROR",
    "source": "auth-service",
    "type": "security",
    "message": "Failed login attempt detected"
  }'
echo ""

# Test 2: Send performance metric
echo "Test 2: Sending performance metric..."
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "severity": "INFO",
    "source": "api-gateway",
    "type": "metric",
    "message": "response_time=250ms",
    "metadata": {"responseTime": 250}
  }'
echo ""

# Test 3: Send application error
echo "Test 3: Sending application error..."
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "severity": "FATAL",
    "source": "payment-api",
    "type": "application",
    "message": "Database connection failed"
  }'
echo ""

# Test 4: Get routing rules
echo "Test 4: Fetching routing rules..."
curl http://localhost:8081/api/rules
echo ""

echo "Integration tests completed!"
