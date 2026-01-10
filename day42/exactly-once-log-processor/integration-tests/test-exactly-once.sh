#!/bin/bash

echo "=================================="
echo "Testing Exactly-Once Semantics"
echo "=================================="

GATEWAY_URL="http://localhost:8080"
PRODUCER_URL="http://localhost:8081"

# Test 1: Send duplicate events
echo -e "\n[Test 1] Sending duplicate log events..."

EVENT_ID="test-event-$(date +%s)"

for i in {1..3}; do
    curl -s -X POST "$GATEWAY_URL/api/logs" \
        -H "Content-Type: application/json" \
        -d "{
            \"eventId\": \"$EVENT_ID\",
            \"eventType\": \"ERROR\",
            \"service\": \"test-service\",
            \"message\": \"Duplicate test event\",
            \"severity\": \"HIGH\",
            \"userId\": \"user123\"
        }" | jq '.'
    sleep 2
done

echo -e "\n✓ Sent 3 duplicate events with ID: $EVENT_ID"
echo "  Check consumer logs for duplicate detection"

# Test 2: Batch send
echo -e "\n[Test 2] Testing atomic batch send..."

curl -s -X POST "$PRODUCER_URL/api/logs/batch" \
    -H "Content-Type: application/json" \
    -d '[
        {
            "eventType": "INFO",
            "service": "batch-test",
            "message": "Batch event 1",
            "severity": "LOW"
        },
        {
            "eventType": "WARN",
            "service": "batch-test",
            "message": "Batch event 2",
            "severity": "MEDIUM"
        },
        {
            "eventType": "ERROR",
            "service": "batch-test",
            "message": "Batch event 3",
            "severity": "HIGH"
        }
    ]' | jq '.'

echo -e "\n✓ Batch send completed"

# Test 3: Check idempotency cache
echo -e "\n[Test 3] Checking Redis idempotency cache..."

REDIS_CONTAINER=$(docker ps --filter "name=redis" --format "{{.Names}}" | grep -i redis | head -1)
if [ -n "$REDIS_CONTAINER" ]; then
    docker exec "$REDIS_CONTAINER" redis-cli KEYS "idempotency:*" | head -10
    echo -e "\n✓ Idempotency keys stored in Redis"
else
    echo "⚠️  Redis container not found"
fi

# Test 4: Verify metrics
echo -e "\n[Test 4] Checking Prometheus metrics..."

echo "Producer success count:"
curl -s "http://localhost:8081/actuator/prometheus" | grep "log_producer_send_success_total"

echo -e "\nConsumer processed count:"
curl -s "http://localhost:8082/actuator/prometheus" | grep "log_consumer_processed_total"

echo -e "\nConsumer duplicate count:"
curl -s "http://localhost:8082/actuator/prometheus" | grep "log_consumer_duplicates_total"

echo -e "\n=================================="
echo "Integration Tests Complete"
echo "=================================="
echo "Next steps:"
echo "1. Check Grafana dashboard at http://localhost:3000 (admin/admin)"
echo "2. View Prometheus at http://localhost:9090"
echo "3. Query PostgreSQL: docker exec -it exactly-once-log-processor-postgres-1 psql -U postgres -d logprocessor"
echo "=================================="
