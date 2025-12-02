#!/bin/bash

echo "🔥 Starting load test..."

GATEWAY_URL="http://localhost:8080/api/write"
DURATION=60
CONCURRENT=10

echo "Testing $DURATION seconds with $CONCURRENT concurrent connections"

# Function to send requests
send_requests() {
  local id=$1
  local count=0
  local start=$(date +%s)
  local end=$((start + DURATION))
  
  while [ $(date +%s) -lt $end ]; do
    curl -s -X POST $GATEWAY_URL \
      -H "Content-Type: application/json" \
      -d "{\"data\":\"Load test message $id-$count\"}" \
      > /dev/null
    count=$((count + 1))
  done
  
  echo "Worker $id sent $count requests"
}

# Start concurrent workers
for i in $(seq 1 $CONCURRENT); do
  send_requests $i &
done

# Wait for all workers
wait

echo "✅ Load test complete"

# Show results
echo ""
echo "Cluster status:"
curl -s http://localhost:8080/api/status | python3 -m json.tool
