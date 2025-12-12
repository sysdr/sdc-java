#!/bin/bash

# Test system behavior during replica failures

BASE_URL="http://localhost:8080/api/logs"

echo "=== Failure Scenario Testing ==="
echo

# Write with QUORUM before failures
echo "Step 1: Write with QUORUM (all replicas healthy)"
curl -X POST "$BASE_URL?consistency=QUORUM" \
  -H "Content-Type: application/json" \
  -d '{"key":"failure-test","value":"before-failure"}'
echo -e "\n"

sleep 2

# Simulate failure by stopping 2 replicas
echo "Step 2: Simulating failure of 2 out of 5 replicas..."
docker-compose stop storage-node-4 storage-node-5
echo "Replicas 4 and 5 stopped"
echo

sleep 3

# Test ONE consistency (should work)
echo "Step 3: Write with ONE consistency (should succeed)"
curl -X POST "$BASE_URL?consistency=ONE" \
  -H "Content-Type: application/json" \
  -d '{"key":"failure-test-one","value":"with-failures"}'
echo -e "\n"

sleep 1

# Test QUORUM consistency (should work with 3/5 replicas)
echo "Step 4: Write with QUORUM consistency (should succeed with 3/5)"
curl -X POST "$BASE_URL?consistency=QUORUM" \
  -H "Content-Type: application/json" \
  -d '{"key":"failure-test-quorum","value":"with-failures"}'
echo -e "\n"

sleep 1

# Test ALL consistency (should fail)
echo "Step 5: Write with ALL consistency (should fail - only 3/5 available)"
curl -X POST "$BASE_URL?consistency=ALL" \
  -H "Content-Type: application/json" \
  -d '{"key":"failure-test-all","value":"with-failures"}'
echo -e "\n"

echo "Step 6: Recovering failed replicas..."
docker-compose start storage-node-4 storage-node-5
echo "Replicas 4 and 5 restarted"
echo

sleep 10

echo "Step 7: Verify ALL consistency works again"
curl -X POST "$BASE_URL?consistency=ALL" \
  -H "Content-Type: application/json" \
  -d '{"key":"failure-test-recovered","value":"after-recovery"}'
echo -e "\n"

echo "=== Failure Test Complete ==="
echo
echo "Key findings:"
echo "  - ONE consistency: Always available (accepts writes to any 1 replica)"
echo "  - QUORUM consistency: Available with 3/5 replicas (tolerates 2 failures)"
echo "  - ALL consistency: Requires all replicas (no fault tolerance)"
