#!/bin/bash

echo "=== Load Testing Log Ingestion ==="

GATEWAY_URL="http://localhost:8080/gateway/logs"
NUM_REQUESTS=100

echo "Sending $NUM_REQUESTS requests to $GATEWAY_URL..."

for i in $(seq 1 $NUM_REQUESTS); do
    curl -X POST $GATEWAY_URL \
        -H "Content-Type: application/json" \
        -d "{\"level\":\"INFO\",\"message\":\"Load test message $i\",\"source\":\"load-test\"}" \
        -s -o /dev/null -w "Request $i: %{http_code} (%{time_total}s)\n" &
    
    # Small delay to avoid overwhelming the system
    if [ $((i % 10)) -eq 0 ]; then
        wait
        sleep 1
    fi
done

wait
echo "Load test complete!"
