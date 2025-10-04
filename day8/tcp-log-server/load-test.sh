#!/bin/bash

# Load test script for TCP log server

HOST="localhost"
PORT="9090"
DURATION=60  # seconds
RATE=100     # messages per second

echo "Starting load test..."
echo "Target: $HOST:$PORT"
echo "Duration: $DURATION seconds"
echo "Rate: $RATE messages/second"

start_time=$(date +%s)
end_time=$((start_time + DURATION))
count=0

while [ $(date +%s) -lt $end_time ]; do
    timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    # Generate log message
    message="{\"timestamp\":\"$timestamp\",\"level\":\"INFO\",\"message\":\"Load test message $count\",\"source\":\"load-test\",\"metadata\":{\"test_id\":$count}}"
    
    # Send via netcat (non-blocking)
    echo "$message" | nc -w 0 $HOST $PORT &
    
    count=$((count + 1))
    
    # Sleep to maintain rate
    sleep $(echo "scale=3; 1.0/$RATE" | bc)
done

# Wait for background processes
wait

echo "Load test complete: Sent $count messages"

# Get final stats
echo "Fetching server stats..."
curl -s "http://localhost:8080/api/logs/stats?start=$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)&end=$(date -u +%Y-%m-%dT%H:%M:%SZ)" | jq .
