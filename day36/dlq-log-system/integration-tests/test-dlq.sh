#!/bin/bash

API_URL="http://localhost:8081/api/logs"
GATEWAY_URL="http://localhost:8080/api/dlq"

echo "=== Dead Letter Queue Integration Tests ==="

# Test 1: Normal message processing
echo "Test 1: Publishing normal message..."
curl -s -X POST "$API_URL/event" \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "service": "test-service",
    "message": "Normal test message",
    "shouldFail": false
  }' | jq .

sleep 2

# Test 2: Message that triggers validation error (immediate DLQ)
echo -e "\nTest 2: Publishing message with validation error..."
curl -s -X POST "$API_URL/event" \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR",
    "service": "test-service",
    "message": "validation error test",
    "shouldFail": true
  }' | jq .

sleep 2

# Test 3: Message that triggers timeout error (retry then DLQ)
echo -e "\nTest 3: Publishing message with timeout error..."
curl -s -X POST "$API_URL/event" \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR",
    "service": "test-service",
    "message": "timeout error test",
    "shouldFail": true
  }' | jq .

sleep 5

# Test 4: Batch with failure rate
echo -e "\nTest 4: Publishing batch with 10% failure rate..."
curl -s -X POST "$API_URL/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 100,
    "failureRate": 10
  }' | jq .

sleep 10

# Test 5: Check DLQ stats
echo -e "\nTest 5: Checking DLQ statistics..."
curl -s "$GATEWAY_URL/stats" | jq .

# Test 6: List DLQ messages
echo -e "\nTest 6: Listing DLQ messages..."
curl -s "$GATEWAY_URL/messages?limit=5" | jq .

# Test 7: Reprocess a message
echo -e "\nTest 7: Reprocessing first DLQ message..."
MESSAGE_ID=$(curl -s "$GATEWAY_URL/messages?limit=1" | jq -r '.[0].messageId')
if [ "$MESSAGE_ID" != "null" ]; then
  curl -s -X POST "$GATEWAY_URL/reprocess/$MESSAGE_ID" | jq .
fi

echo -e "\n=== Tests Complete ==="
