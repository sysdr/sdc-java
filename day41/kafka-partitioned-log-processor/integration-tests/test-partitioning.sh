#!/bin/bash

echo "Testing Kafka Partitioning and Consumer Groups..."

GATEWAY_URL="http://localhost:8080"

# Test 1: Send logs from multiple sources
echo ""
echo "Test 1: Sending logs from 5 different sources..."
for i in {1..5}; do
  for j in {1..10}; do
    curl -X POST "$GATEWAY_URL/api/logs" \
      -H "Content-Type: application/json" \
      -d "{
        \"source\": \"app-server-$i\",
        \"level\": \"INFO\",
        \"message\": \"Log message $j from server $i\",
        \"application\": \"test-app\",
        \"hostname\": \"host-$i\"
      }" &
  done
done

wait
echo "Sent 50 log events from 5 sources"

sleep 5

# Test 2: Check partition mapping
echo ""
echo "Test 2: Checking partition mapping..."
curl -s "$GATEWAY_URL/api/partition-mapping" | jq '.'

# Test 3: Check consumer lag
echo ""
echo "Test 3: Checking consumer lag..."
curl -s "$GATEWAY_URL/api/consumer/lag" | jq '.'

# Test 4: Send high volume to one source
echo ""
echo "Test 4: Testing hot partition scenario..."
for i in {1..100}; do
  curl -X POST "$GATEWAY_URL/api/logs" \
    -H "Content-Type: application/json" \
    -d "{
      \"source\": \"hot-source\",
      \"level\": \"WARN\",
      \"message\": \"High volume message $i\",
      \"application\": \"load-test\",
      \"hostname\": \"hot-host\"
    }" &
done

wait
echo "Sent 100 events to single source"

sleep 5

echo ""
echo "Test 5: Final partition distribution..."
curl -s "$GATEWAY_URL/api/partition-mapping" | jq '.'

echo ""
echo "Test 6: Consumer health check..."
curl -s "$GATEWAY_URL/api/consumer/health" | jq '.'

echo ""
echo "Integration tests completed!"
