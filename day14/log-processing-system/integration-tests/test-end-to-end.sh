#!/bin/bash

set -e

echo "Running End-to-End Integration Tests"
echo "====================================="
echo ""

# Test 1: Health checks
echo "Test 1: Checking service health..."
for port in 8080 8081 8082; do
    response=$(curl -s "http://localhost:$port/actuator/health")
    status=$(echo $response | jq -r '.status')
    if [ "$status" = "UP" ]; then
        echo "  ✓ Service on port $port is healthy"
    else
        echo "  ✗ Service on port $port is not healthy"
        exit 1
    fi
done
echo ""

# Test 2: Generate small burst
echo "Test 2: Generating test logs..."
curl -s -X POST "http://localhost:8081/api/load/burst?durationSeconds=5" > /dev/null
sleep 8
stats=$(curl -s "http://localhost:8081/api/load/stats")
count=$(echo $stats | jq -r '.totalGenerated')
if [ $count -gt 0 ]; then
    echo "  ✓ Generated $count logs successfully"
else
    echo "  ✗ Failed to generate logs"
    exit 1
fi
echo ""

# Test 3: Check processing metrics
echo "Test 3: Verifying log processing..."
sleep 5
metrics=$(curl -s "http://localhost:8082/actuator/metrics/log.processed.count")
processed=$(echo $metrics | jq -r '.measurements[0].value')
if [ $(echo "$processed > 0" | bc) -eq 1 ]; then
    echo "  ✓ Processed $processed logs"
else
    echo "  ✗ No logs processed"
    exit 1
fi
echo ""

# Test 4: Gateway aggregation
echo "Test 4: Testing gateway metrics aggregation..."
gateway_response=$(curl -s "http://localhost:8080/api/metrics/summary")
if [ $? -eq 0 ]; then
    echo "  ✓ Gateway successfully aggregated metrics"
else
    echo "  ✗ Gateway metrics aggregation failed"
    exit 1
fi
echo ""

echo "====================================="
echo "All integration tests passed! ✓"
echo "====================================="
