#!/bin/bash

echo "🔥 Starting Load Test for Failover System"
echo "=========================================="

DURATION=60
CONCURRENCY=10
REQUESTS_PER_SECOND=50

echo "Duration: ${DURATION}s"
echo "Concurrency: $CONCURRENCY"
echo "Target RPS: $REQUESTS_PER_SECOND"
echo ""

# Generate random log messages
generate_message() {
    LEVELS=("INFO" "WARN" "ERROR" "DEBUG")
    LEVEL=${LEVELS[$RANDOM % ${#LEVELS[@]}]}
    MESSAGE="Load test message $(date +%s%N)"
    SOURCE="load-test-client"
    
    echo "{\"level\":\"$LEVEL\",\"message\":\"$MESSAGE\",\"source\":\"$SOURCE\"}"
}

# Run load test
echo "Starting load test..."
START_TIME=$(date +%s)

for i in $(seq 1 $((DURATION * REQUESTS_PER_SECOND))); do
    PAYLOAD=$(generate_message)
    
    curl -s -X POST http://localhost:8080/api/logs \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD" > /dev/null &
    
    # Control concurrency
    if (( i % CONCURRENCY == 0 )); then
        wait
    fi
    
    # Control rate
    sleep 0.02
    
    if (( i % 100 == 0 )); then
        echo "Sent $i requests..."
    fi
done

wait

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo "Load test completed!"
echo "Total time: ${ELAPSED}s"
echo "Actual RPS: $((DURATION * REQUESTS_PER_SECOND / ELAPSED))"
echo ""
echo "Check Grafana dashboard at http://localhost:3000"
