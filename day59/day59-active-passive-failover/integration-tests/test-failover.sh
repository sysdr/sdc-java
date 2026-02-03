#!/bin/bash

echo "🧪 Testing Active-Passive Failover System"
echo "=========================================="

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Test 1: Check system status
echo ""
echo "Test 1: Checking system status..."
SYSTEM_STATUS=$(curl -s http://localhost:8080/api/system/status)
echo "System Status: $SYSTEM_STATUS"

LEADER=$(echo $SYSTEM_STATUS | jq -r '.currentLeader')
echo "Current Leader: $LEADER"

# Test 2: Publish test messages
echo ""
echo "Test 2: Publishing test messages..."
for i in {1..10}; do
    MESSAGE_ID=$(curl -s -X POST http://localhost:8080/api/logs \
        -H "Content-Type: application/json" \
        -d "{\"level\":\"INFO\",\"message\":\"Test message $i\",\"source\":\"test-client\"}" \
        | jq -r '.messageId')
    echo "Published message: $MESSAGE_ID"
    sleep 0.1
done

# Test 3: Check consumer status
echo ""
echo "Test 3: Checking consumer status..."
sleep 5
CONSUMER_STATUS=$(curl -s http://localhost:8081/api/failover/status)
echo "Consumer Status: $CONSUMER_STATUS"

MESSAGES_PROCESSED=$(echo $CONSUMER_STATUS | jq -r '.messagesProcessed')
echo "Messages Processed: $MESSAGES_PROCESSED"

# Test 4: Check metrics
echo ""
echo "Test 4: Checking Prometheus metrics..."
METRICS=$(curl -s http://localhost:8081/actuator/prometheus | grep -E "(failover|messages|leader)")
echo "Key Metrics:"
echo "$METRICS" | grep "failover_events_total"
echo "$METRICS" | grep "messages_processed_total"
echo "$METRICS" | grep "leader_status"

echo ""
echo "✅ Integration tests completed!"
echo ""
echo "To simulate failover:"
echo "1. Note the current leader instance ID"
echo "2. Kill the log-consumer process"
echo "3. Watch logs of another consumer instance take over"
echo "4. Verify messages continue processing"
