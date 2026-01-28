#!/bin/bash

set -e

echo "=== Distributed Indexing Integration Tests ==="

ROUTER_URL="http://localhost:8090"
COORDINATOR_URL="http://localhost:8080"

# Wait for services
echo "Waiting for services to be ready..."
sleep 30

# Test 1: Check hash ring distribution
echo ""
echo "Test 1: Checking hash ring distribution..."
curl -s "$ROUTER_URL/api/ring/distribution?samples=10000" | jq '.'

# Test 2: Send test logs through router
echo ""
echo "Test 2: Sending 100 test logs through router..."
for i in {1..100}; do
  curl -s -X POST "$ROUTER_URL/api/route" \
    -H "Content-Type: application/json" \
    -d "{
      \"logId\": \"test_log_$i\",
      \"tenantId\": \"tenant_1\",
      \"timestamp\": $(date +%s)000,
      \"level\": \"INFO\",
      \"message\": \"Integration test message $i\",
      \"service\": \"test-service\"
    }" > /dev/null
done
echo "Sent 100 logs"

# Wait for indexing
sleep 5

# Test 3: Query through coordinator
echo ""
echo "Test 3: Searching for 'integration test' across all shards..."
SEARCH_RESULT=$(curl -s "$COORDINATOR_URL/api/search?q=integration&limit=10")
echo "$SEARCH_RESULT" | jq '.'

TOTAL_HITS=$(echo "$SEARCH_RESULT" | jq '.totalHits')
echo "Total hits across shards: $TOTAL_HITS"

# Test 4: Check individual node stats
echo ""
echo "Test 4: Checking stats for each index node..."
for port in 8081 8082 8083; do
  echo "Node at port $port:"
  curl -s "http://localhost:$port/api/stats" | jq '.'
done

# Test 5: Verify scatter-gather by querying coordinator
echo ""
echo "Test 5: Testing scatter-gather with broader query..."
curl -s "$COORDINATOR_URL/api/search?q=test&limit=50" | jq '{totalHits, shardsQueried, shardsSucceeded, partial}'

echo ""
echo "=== All Integration Tests Completed ==="
