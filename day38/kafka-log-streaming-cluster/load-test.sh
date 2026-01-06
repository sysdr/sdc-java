#!/bin/bash

echo "Running Kafka Load Test"
echo "======================="

# Test 1: Low volume
echo "Test 1: Sending 1,000 events..."
curl -X POST "http://localhost:8083/api/loadtest/run?eventCount=1000&topic=log-events"
echo ""
sleep 2

# Test 2: Medium volume
echo "Test 2: Sending 10,000 events..."
curl -X POST "http://localhost:8083/api/loadtest/run?eventCount=10000&topic=log-events"
echo ""
sleep 2

# Test 3: High volume
echo "Test 3: Sending 50,000 events..."
curl -X POST "http://localhost:8083/api/loadtest/run?eventCount=50000&topic=log-events"
echo ""

# Get stats
echo "Getting statistics..."
curl -s http://localhost:8083/api/loadtest/stats | python3 -m json.tool
echo ""

echo "Load test complete! View metrics at http://localhost:3000"
