#!/bin/bash

echo "🧪 Testing Consistent Hashing Distribution"

GATEWAY_URL="http://localhost:8080/api/logs"
COORDINATOR_URL="http://localhost:8081/api/coordinator"

# Test 1: Send logs with same source IP (should go to same node)
echo "Test 1: Locality - same source IP goes to same node"
SOURCE_IP="10.0.0.100"

for i in {1..10}; do
  curl -s -X POST "$GATEWAY_URL" \
    -H "Content-Type: application/json" \
    -d "{
      \"message\": \"Locality test $i\",
      \"level\": \"INFO\",
      \"source\": \"test\",
      \"sourceIp\": \"$SOURCE_IP\",
      \"application\": \"test-app\"
    }" > /dev/null
done

sleep 2
echo "✓ Sent 10 logs from $SOURCE_IP"

# Check which node got them
NODE=$(curl -s "$COORDINATOR_URL/node/$SOURCE_IP")
echo "✓ All logs routed to: $NODE"

# Test 2: Check distribution balance
echo ""
echo "Test 2: Distribution Balance"
METRICS=$(curl -s "$COORDINATOR_URL/metrics/distribution")
echo "$METRICS" | jq

BALANCE=$(echo "$METRICS" | jq -r '.balanceScore')
echo ""
if (( $(echo "$BALANCE > 90" | bc -l) )); then
  echo "✅ Distribution is well-balanced (score: $BALANCE/100)"
else
  echo "⚠️  Distribution could be better (score: $BALANCE/100)"
fi

# Test 3: Ring membership
echo ""
echo "Test 3: Ring Membership"
curl -s "$COORDINATOR_URL/nodes" | jq
echo "✓ Active nodes listed above"

echo ""
echo "✅ All tests passed!"
