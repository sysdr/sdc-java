#!/bin/bash

echo "Starting Protocol Buffers Load Test..."
echo "This will compare JSON vs Protobuf performance"
echo ""

PRODUCER_URL="http://localhost:8081/api/logs"
PROTOBUF_URL="http://localhost:8081/api/logs/binary"
NUM_REQUESTS=1000
CONCURRENCY=10

# Generate sample protobuf message
# Note: In production, you'd use protoc to generate binary
# For this test, we'll use JSON endpoint

echo "Test 1: JSON Format"
echo "===================="

START_TIME=$(date +%s)

for i in $(seq 1 $NUM_REQUESTS); do
  curl -X POST "$PRODUCER_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventId": "json-'$i'",
      "timestamp": "'$(date -Iseconds)'",
      "level": "INFO",
      "message": "Load test message '$i'",
      "serviceName": "load-tester",
      "environment": "test",
      "tags": {"test": "json", "iteration": "'$i'"}
    }' \
    -s -o /dev/null &
  
  if [ $((i % $CONCURRENCY)) -eq 0 ]; then
    wait
  fi
done

wait

END_TIME=$(date +%s)
JSON_DURATION=$((END_TIME - START_TIME))

echo "JSON Test completed in $JSON_DURATION seconds"
echo "Throughput: $((NUM_REQUESTS / JSON_DURATION)) requests/sec"
echo ""

# Wait between tests
sleep 5

echo "Test 2: Analyzing Metrics"
echo "========================="
echo "Check Prometheus at http://localhost:9090"
echo "Query: rate(log_events_json_total[1m])"
echo "Query: rate(log_events_protobuf_total[1m])"
echo ""
echo "Check Grafana at http://localhost:3000"
echo "Dashboard: Protocol Buffers Performance"
