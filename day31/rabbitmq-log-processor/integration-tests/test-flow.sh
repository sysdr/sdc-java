#!/bin/bash

echo "🧪 Running Integration Tests..."

GATEWAY_URL="http://localhost:8080"

# Test 1: Single log publish
echo "Test 1: Publishing single log..."
RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/logs" \
  -H "Content-Type: application/json" \
  -d '{
    "severity": "error",
    "category": "auth",
    "message": "Authentication failed for user",
    "source": "auth-service"
  }')

if echo "$RESPONSE" | grep -q "published"; then
  echo "✅ Test 1 passed"
else
  echo "❌ Test 1 failed: $RESPONSE"
fi

# Test 2: Batch publish
echo "Test 2: Publishing batch logs..."
RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/logs/batch" \
  -H "Content-Type: application/json" \
  -d '[
    {"severity": "info", "category": "payment", "message": "Payment processed", "source": "payment-service"},
    {"severity": "warn", "category": "inventory", "message": "Low stock alert", "source": "inventory-service"},
    {"severity": "error", "category": "payment", "message": "Payment gateway timeout", "source": "payment-service"}
  ]')

if echo "$RESPONSE" | grep -q "published"; then
  echo "✅ Test 2 passed"
else
  echo "❌ Test 2 failed: $RESPONSE"
fi

echo "Waiting 5 seconds for message processing..."
sleep 5

# Test 3: Check RabbitMQ queues
echo "Test 3: Checking RabbitMQ queue status..."
QUEUE_STATUS=$(curl -s -u admin:admin123 "http://localhost:15672/api/queues/%2F/logs-critical")

if echo "$QUEUE_STATUS" | grep -q "logs-critical"; then
  echo "✅ Test 3 passed"
else
  echo "❌ Test 3 failed"
fi

echo "✨ Integration tests complete!"
