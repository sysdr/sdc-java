#!/bin/bash

echo "Testing Kafka Cluster Health"
echo "============================"

# Test admin service
echo "1. Testing Admin Service..."
TOPICS=$(curl -s http://localhost:8081/api/topics)
if echo "$TOPICS" | grep -q "log-events"; then
    echo "   ✓ Admin service working, topics created"
else
    echo "   ✗ Admin service failed"
    exit 1
fi

# Test health service
echo "2. Testing Health Service..."
HEALTH=$(curl -s http://localhost:8082/api/health/status)
if echo "$HEALTH" | grep -q "HEALTHY\|DEGRADED"; then
    echo "   ✓ Health service working, cluster status: $HEALTH"
else
    echo "   ✗ Health service failed"
    exit 1
fi

# Test producer
echo "3. Testing Load Test Service..."
RESULT=$(curl -s -X POST "http://localhost:8083/api/loadtest/run?eventCount=100&topic=log-events")
if echo "$RESULT" | grep -q "throughput"; then
    echo "   ✓ Producer working, events sent successfully"
else
    echo "   ✗ Producer failed"
    exit 1
fi

echo ""
echo "All tests passed! ✓"
