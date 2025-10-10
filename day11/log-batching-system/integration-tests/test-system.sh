#!/bin/bash

echo "Running integration tests..."

# Wait for services to be ready
sleep 30

# Test 1: Check all services are healthy
echo "Test 1: Health checks..."
curl -f http://localhost:8081/actuator/health || exit 1
curl -f http://localhost:8082/api/logs/health || exit 1
curl -f http://localhost:8083/actuator/health || exit 1
curl -f http://localhost:8080/api/query/health || exit 1

# Test 2: Wait for logs to flow through system
echo "Test 2: Waiting for log processing..."
sleep 60

# Test 3: Query recent logs
echo "Test 3: Querying recent logs..."
RECENT_LOGS=$(curl -s http://localhost:8080/api/query/recent)
LOG_COUNT=$(echo $RECENT_LOGS | jq 'length')

if [ "$LOG_COUNT" -gt 0 ]; then
    echo "✓ Found $LOG_COUNT logs in system"
else
    echo "✗ No logs found in system"
    exit 1
fi

# Test 4: Check metrics
echo "Test 4: Checking metrics..."
METRICS=$(curl -s http://localhost:8082/actuator/prometheus)
if echo "$METRICS" | grep -q "logs_sent_total"; then
    echo "✓ Metrics endpoint working"
else
    echo "✗ Metrics not found"
    exit 1
fi

echo "✓ All integration tests passed!"
