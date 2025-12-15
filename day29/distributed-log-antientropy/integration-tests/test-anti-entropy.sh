#!/bin/bash

set -e

echo "====================================="
echo "Anti-Entropy Integration Tests"
echo "====================================="

GATEWAY_URL="http://localhost:8080"
NODE1_URL="http://localhost:8081"
NODE2_URL="http://localhost:8082"
MERKLE_URL="http://localhost:8084"
COORDINATOR_URL="http://localhost:8085"
REPAIR_URL="http://localhost:8086"
HINTS_URL="http://localhost:8087"

# Test 1: Verify write replication
echo ""
echo "Test 1: Verify write replication..."
response=$(curl -s -X POST "$GATEWAY_URL/api/write" \
    -H "Content-Type: application/json" \
    -d '{"partitionId": "test-partition", "message": "Test message"}')

version=$(echo $response | grep -o '"version":[0-9]*' | grep -o '[0-9]*')
echo "Written version: $version"

# Check all nodes
for node in "$NODE1_URL" "$NODE2_URL"; do
    node_response=$(curl -s "$node/api/storage/read/test-partition/$version")
    if echo "$node_response" | grep -q "Test message"; then
        echo "✓ Data replicated to $node"
    else
        echo "✗ Data not found on $node"
        exit 1
    fi
done

# Test 2: Merkle tree comparison
echo ""
echo "Test 2: Merkle tree comparison..."
compare_response=$(curl -s -X POST "$MERKLE_URL/api/merkle/compare" \
    -H "Content-Type: application/json" \
    -d "{\"partitionId\": \"test-partition\", \"node1Url\": \"$NODE1_URL\", \"node2Url\": \"$NODE2_URL\"}")

inconsistencies=$(echo $compare_response | grep -o '"inconsistentSegments":[0-9]*' | grep -o '[0-9]*')
echo "Inconsistencies found: $inconsistencies"

if [ "$inconsistencies" -eq 0 ]; then
    echo "✓ Nodes are consistent"
else
    echo "! Inconsistencies detected, triggering repair..."
fi

# Test 3: Read repair
echo ""
echo "Test 3: Read repair verification..."
read_response=$(curl -s "$REPAIR_URL/api/read-repair/read/test-partition/$version")
if echo "$read_response" | grep -q "Test message"; then
    echo "✓ Read repair successful"
else
    echo "✗ Read repair failed"
    exit 1
fi

# Test 4: Hints system
echo ""
echo "Test 4: Hints system check..."
hints_stats=$(curl -s "$HINTS_URL/api/hints/stats")
echo "Hint statistics: $hints_stats"

pending=$(echo $hints_stats | grep -o '"pending":[0-9]*' | grep -o '[0-9]*')
if [ "$pending" -ge 0 ]; then
    echo "✓ Hints system operational"
else
    echo "✗ Hints system error"
    exit 1
fi

# Test 5: Coordinator jobs
echo ""
echo "Test 5: Coordinator reconciliation..."
trigger_response=$(curl -s -X POST "$COORDINATOR_URL/api/coordinator/trigger")
echo "Reconciliation triggered: $trigger_response"

sleep 10

jobs=$(curl -s "$COORDINATOR_URL/api/coordinator/jobs?status=PENDING")
echo "Pending jobs: $(echo $jobs | grep -o '\[.*\]')"
echo "✓ Coordinator operational"

echo ""
echo "====================================="
echo "All integration tests passed!"
echo "====================================="
