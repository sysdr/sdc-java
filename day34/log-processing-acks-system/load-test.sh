#!/bin/bash

GATEWAY_URL="http://localhost:8080"
TOTAL_MESSAGES=10000
CONCURRENT=50

echo "=========================================="
echo "Running Load Test"
echo "Total Messages: $TOTAL_MESSAGES"
echo "Concurrent Requests: $CONCURRENT"
echo "=========================================="

send_message() {
  local id=$1
  curl -s -X POST $GATEWAY_URL/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"level\":\"INFO\",\"message\":\"Load test $id\",\"source\":\"load-test\",\"userId\":\"user-$((id % 100))\",\"simulateFailure\":$((RANDOM % 10 == 0))}" > /dev/null
}

export -f send_message
export GATEWAY_URL

start_time=$(date +%s)

seq 1 $TOTAL_MESSAGES | xargs -P $CONCURRENT -I {} bash -c 'send_message {}'

end_time=$(date +%s)
duration=$((end_time - start_time))

echo "=========================================="
echo "Load Test Completed"
echo "Duration: ${duration}s"
echo "Throughput: $((TOTAL_MESSAGES / duration)) msg/s"
echo "=========================================="
