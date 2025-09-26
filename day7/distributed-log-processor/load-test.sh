#!/bin/bash

set -e

echo "🚀 Starting load test..."

PRODUCER_URL="http://localhost:8081/producer/api/logs/ingest"
GATEWAY_URL="http://localhost:8080/gateway/api/query/logs"
DURATION=60
RATE=10

echo "Generating load for $DURATION seconds at $RATE requests per second..."

# Function to generate random log
generate_log() {
    local id=$(uuidgen)
    local levels=("INFO" "WARN" "ERROR" "DEBUG")
    local sources=("web-app" "api-server" "background-job" "scheduler")
    local level=${levels[$RANDOM % ${#levels[@]}]}
    local source=${sources[$RANDOM % ${#sources[@]}]}
    
    echo "{
        \"id\": \"$id\",
        \"level\": \"$level\",
        \"message\": \"Load test message $RANDOM\",
        \"source\": \"$source\",
        \"metadata\": {
            \"loadTest\": true,
            \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\"
        }
    }"
}

# Start load generation
echo "Starting log ingestion load test..."
for i in $(seq 1 $((DURATION * RATE))); do
    generate_log | curl -X POST "$PRODUCER_URL" \
        -H "Content-Type: application/json" \
        -d @- > /dev/null 2>&1 &
    
    if (( i % 10 == 0 )); then
        echo "Sent $i requests..."
    fi
    
    sleep $(echo "scale=2; 1/$RATE" | bc -l)
done

wait
echo "✅ Load test completed!"

# Test query performance
echo "Testing query performance..."
time curl -s "$GATEWAY_URL" > /dev/null
echo "✅ Query performance test completed!"
