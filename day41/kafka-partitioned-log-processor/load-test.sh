#!/bin/bash

GATEWAY_URL="http://localhost:8080"
TOTAL_REQUESTS=10000
CONCURRENT=50

echo "Starting load test: $TOTAL_REQUESTS requests with $CONCURRENT concurrent connections"

SOURCES=("app-1" "app-2" "app-3" "app-4" "app-5" "app-6" "app-7" "app-8")

for i in $(seq 1 $TOTAL_REQUESTS); do
  SOURCE=${SOURCES[$RANDOM % ${#SOURCES[@]}]}
  
  curl -X POST "$GATEWAY_URL/api/logs" \
    -H "Content-Type: application/json" \
    -d "{
      \"source\": \"$SOURCE\",
      \"level\": \"INFO\",
      \"message\": \"Load test message $i\",
      \"application\": \"load-test-app\",
      \"hostname\": \"load-host\"
    }" &

  if (( i % CONCURRENT == 0 )); then
    wait
    echo "Progress: $i / $TOTAL_REQUESTS"
  fi
done

wait

echo ""
echo "Load test completed!"
echo "Check Prometheus for throughput metrics: http://localhost:9090"
echo "Check Grafana dashboard: http://localhost:3000"
