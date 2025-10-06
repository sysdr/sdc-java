#!/bin/bash

echo "🚀 Running Load Test on TCP Log Shipping System"

ENDPOINT="http://localhost:8081/api/logs"
DURATION=60
CONCURRENT=10

echo "Endpoint: $ENDPOINT"
echo "Duration: ${DURATION}s"
echo "Concurrent requests: $CONCURRENT"
echo ""

# Function to send logs
send_logs() {
  local id=$1
  local count=0
  
  end_time=$((SECONDS + DURATION))
  
  while [ $SECONDS -lt $end_time ]; do
    curl -s -X POST $ENDPOINT \
      -H "Content-Type: application/json" \
      -d "{
        \"level\": \"INFO\",
        \"message\": \"Load test message from worker $id\",
        \"service\": \"load-test\"
      }" > /dev/null
    
    ((count++))
    
    if [ $((count % 100)) -eq 0 ]; then
      echo "Worker $id: Sent $count logs"
    fi
  done
  
  echo "Worker $id: Complete. Total logs sent: $count"
}

# Start concurrent workers
for i in $(seq 1 $CONCURRENT); do
  send_logs $i &
done

# Wait for all workers to complete
wait

echo ""
echo "✅ Load test complete!"
echo "Check Grafana dashboard at http://localhost:3000"
echo "Check Prometheus metrics at http://localhost:9090"
