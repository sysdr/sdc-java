#!/bin/bash

# Load test script - generates 10,000 requests
echo "Starting load test with 10,000 requests..."

for i in {1..10000}; do
  curl -X POST http://localhost:8080/api/v1/logs \
    -H "Content-Type: application/json" \
    -d "{\"source\": \"load-test\", \"level\": \"INFO\", \"message\": \"Test message $i\"}" \
    -s -o /dev/null &
  
  if (( $i % 100 == 0 )); then
    echo "Sent $i requests..."
    wait
  fi
done

wait
echo "Load test completed!"
