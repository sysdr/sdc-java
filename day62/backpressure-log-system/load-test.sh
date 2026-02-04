#!/bin/bash

echo "Running load test..."
echo "This will generate sustained load to trigger backpressure"

for i in {1..5000}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"correlationId\":\"load-$RANDOM\",\"severity\":\"INFO\",\"message\":\"Load test message $i\",\"source\":\"load-test\",\"timestamp\":$(date +%s)000}" \
    -s -o /dev/null -w "%{http_code} " &
  
  if [ $((i % 100)) -eq 0 ]; then
    echo ""
    echo "Sent $i requests..."
    curl -s http://localhost:8080/api/logs/health | jq .
    sleep 1
  fi
done

wait
echo ""
echo "Load test complete"
