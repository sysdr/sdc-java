#!/bin/bash

echo "====================================="
echo "Anti-Entropy System Load Test"
echo "====================================="

GATEWAY_URL="http://localhost:8080"
PARTITIONS=("partition-1" "partition-2" "partition-3")

# Test 1: Write test
echo ""
echo "Test 1: Writing 100 log entries across partitions..."
for i in {1..100}; do
    partition=${PARTITIONS[$((i % 3))]}
    response=$(curl -s -X POST "$GATEWAY_URL/api/write" \
        -H "Content-Type: application/json" \
        -d "{\"partitionId\": \"$partition\", \"message\": \"Test log entry $i\"}")
    
    if [ $((i % 20)) -eq 0 ]; then
        echo "Completed $i writes..."
    fi
done

echo "✓ Completed 100 writes"

# Test 2: Read test with repair
echo ""
echo "Test 2: Reading entries with read repair..."
for i in {1..20}; do
    partition=${PARTITIONS[$((i % 3))]}
    version=$((i))
    response=$(curl -s "$GATEWAY_URL/api/read/$partition/$version")
    echo "Read $partition version $version: $(echo $response | grep -o '"status":"[^"]*"')"
done

# Test 3: Trigger anti-entropy
echo ""
echo "Test 3: Triggering anti-entropy reconciliation..."
curl -s -X POST "http://localhost:8085/api/coordinator/trigger"
echo "✓ Anti-entropy triggered"

# Wait for reconciliation
echo ""
echo "Waiting 30 seconds for reconciliation to complete..."
sleep 30

# Test 4: Check metrics
echo ""
echo "Test 4: Checking metrics..."
echo ""

echo "Storage Node 1 metrics:"
curl -s "http://localhost:8081/actuator/prometheus" | grep "storage_writes_total\|storage_reads_total"
echo ""

echo "Merkle Tree comparisons:"
curl -s "http://localhost:8084/actuator/prometheus" | grep "merkle_tree"
echo ""

echo "Read repairs:"
curl -s "http://localhost:8086/actuator/prometheus" | grep "read_repairs"
echo ""

echo "Hint statistics:"
curl -s "http://localhost:8087/api/hints/stats"
echo ""

echo "Reconciliation jobs:"
curl -s "http://localhost:8085/api/coordinator/jobs?status=COMPLETED" | python3 -m json.tool
echo ""

echo "====================================="
echo "Load test completed!"
echo "====================================="
echo ""
echo "View detailed metrics:"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000"
echo "====================================="
