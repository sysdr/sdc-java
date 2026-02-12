#!/bin/bash

echo "=== End-to-End Integration Test ==="

# Test 1: Producer health
echo "Test 1: Checking producer health..."
PRODUCER_HEALTH=$(curl -s http://localhost:8081/api/logs/health)
echo "Producer health: $PRODUCER_HEALTH"

# Test 2: Consumer health
echo "Test 2: Checking consumer health..."
CONSUMER_HEALTH=$(curl -s http://localhost:8082/actuator/health | grep -o '"status":"UP"')
echo "Consumer health contains UP: $CONSUMER_HEALTH"

# Test 3: Send log event
echo "Test 3: Sending log event..."
RESPONSE=$(curl -X POST http://localhost:8080/gateway/logs \
    -H "Content-Type: application/json" \
    -d '{"level":"ERROR","message":"Integration test error","source":"integration-test"}' \
    -s)
echo "Response: $RESPONSE"

# Test 4: Verify Prometheus metrics
echo "Test 4: Checking Prometheus metrics..."
METRICS=$(curl -s http://localhost:8081/actuator/prometheus | grep log_events_sent)
echo "Found metrics: $(echo $METRICS | wc -l) lines"

echo ""
echo "=== Integration Tests Complete ==="
