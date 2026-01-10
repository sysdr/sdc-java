#!/bin/bash

echo "=================================="
echo "Load Testing Exactly-Once System"
echo "=================================="

GATEWAY_URL="http://localhost:8080/api/logs"
DURATION=60
CONCURRENT=10

echo "Configuration:"
echo "  Duration: ${DURATION}s"
echo "  Concurrent connections: $CONCURRENT"
echo "  Target: $GATEWAY_URL"

# Generate sample event
generate_event() {
    cat <<JSON
{
    "eventType": "INFO",
    "service": "load-test-service",
    "message": "Load test event at $(date +%s%N)",
    "severity": "MEDIUM",
    "userId": "user-$RANDOM"
}
JSON
}

echo -e "\nStarting load test..."

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))
TOTAL_REQUESTS=0
SUCCESSFUL_REQUESTS=0

# Run load test
while [ $(date +%s) -lt $END_TIME ]; do
    for i in $(seq 1 $CONCURRENT); do
        (
            RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL" \
                -H "Content-Type: application/json" \
                -d "$(generate_event)")
            HTTP_CODE=$(echo "$RESPONSE" | tail -1)
            if [ "$HTTP_CODE" = "200" ]; then
                echo "✓"
            else
                echo "✗ ($HTTP_CODE)"
            fi
        ) &
    done
    wait
    TOTAL_REQUESTS=$((TOTAL_REQUESTS + CONCURRENT))
    echo "Progress: $TOTAL_REQUESTS requests sent..."
    sleep 1
done

echo -e "\n=================================="
echo "Load Test Complete"
echo "Total requests: $TOTAL_REQUESTS"
echo "=================================="

echo -e "\nFetching metrics..."
echo "Producer metrics:"
curl -s "http://localhost:8081/actuator/prometheus" | grep -E "(log_producer_send_success|log_producer_send_failure)"

echo -e "\nConsumer metrics:"
curl -s "http://localhost:8082/actuator/prometheus" | grep -E "(log_consumer_processed|log_consumer_duplicates|log_consumer_errors)"

echo -e "\nCheck Grafana for detailed visualizations: http://localhost:3000"
