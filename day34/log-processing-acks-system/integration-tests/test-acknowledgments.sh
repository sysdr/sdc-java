#!/bin/bash

echo "=========================================="
echo "Testing Message Acknowledgments & Retries"
echo "=========================================="

GATEWAY_URL="http://localhost:8080"

echo -e "\n1. Testing normal message processing..."
curl -X POST $GATEWAY_URL/api/logs \
  -H "Content-Type: application/json" \
  -d '{"level":"INFO","message":"Test normal processing","source":"test-client","userId":"user123"}'

sleep 2

echo -e "\n\n2. Testing message with transient failures (retries)..."
for i in {1..5}; do
  curl -X POST $GATEWAY_URL/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"level\":\"ERROR\",\"message\":\"Test retry $i\",\"source\":\"test\",\"userId\":\"user456\",\"simulateFailure\":true}"
  sleep 1
done

echo -e "\n\n3. Testing idempotency with duplicate messages..."
MESSAGE_ID="duplicate-test-$(date +%s)"
for i in {1..3}; do
  curl -X POST $GATEWAY_URL/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"$MESSAGE_ID\",\"level\":\"WARN\",\"message\":\"Duplicate test $i\",\"source\":\"test\",\"userId\":\"user789\"}"
  sleep 0.5
done

echo -e "\n\n4. Running high-volume load test..."
for i in {1..100}; do
  curl -X POST $GATEWAY_URL/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"level\":\"DEBUG\",\"message\":\"Load test $i\",\"source\":\"load-test\",\"userId\":\"user\"}" &
done

wait

echo -e "\n\n=========================================="
echo "Tests completed!"
echo "Check Grafana at http://localhost:3000"
echo "=========================================="
