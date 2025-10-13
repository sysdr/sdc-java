#!/bin/bash

echo "🧪 Running compression integration tests..."

# Test 1: Send log and verify receipt
echo "Test 1: Basic log transmission..."
RESPONSE=$(curl -X POST http://localhost:8081/api/logs \
    -H "Content-Type: application/json" \
    -d '{"level":"INFO","service":"test","message":"Integration test"}' \
    -s)

if echo "$RESPONSE" | grep -q "accepted"; then
    echo "✅ Test 1 passed"
else
    echo "❌ Test 1 failed"
    exit 1
fi

# Test 2: Verify metrics endpoint
echo "Test 2: Metrics collection..."
METRICS=$(curl -s http://localhost:8081/actuator/prometheus)

if echo "$METRICS" | grep -q "compression_bytes"; then
    echo "✅ Test 2 passed"
else
    echo "❌ Test 2 failed"
    exit 1
fi

# Test 3: Health checks
echo "Test 3: Service health..."
PRODUCER_HEALTH=$(curl -s http://localhost:8081/actuator/health)
CONSUMER_HEALTH=$(curl -s http://localhost:8082/actuator/health)

if echo "$PRODUCER_HEALTH" | grep -q "UP" && echo "$CONSUMER_HEALTH" | grep -q "UP"; then
    echo "✅ Test 3 passed"
else
    echo "❌ Test 3 failed"
    exit 1
fi

echo "✅ All integration tests passed!"
