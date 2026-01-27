#!/bin/bash

echo "⚡ Load Testing Alert Generation System"
echo "======================================"

API_URL="http://localhost:8080"
DURATION=60
RATE=1000

echo "Sending $RATE logs/second for $DURATION seconds..."

END=$((SECONDS+DURATION))
COUNT=0

while [ $SECONDS -lt $END ]; do
  for i in $(seq 1 $RATE); do
    LEVEL=$(shuf -n 1 -e "INFO" "INFO" "INFO" "WARN" "ERROR")
    STATUS=$(shuf -n 1 -e "200" "200" "200" "400" "500")
    LATENCY=$((RANDOM % 5000))
    
    curl -s -X POST ${API_URL}/api/logs \
      -H "Content-Type: application/json" \
      -d "{
        \"service\": \"load-test-service\",
        \"level\": \"$LEVEL\",
        \"message\": \"Load test message $COUNT\",
        \"statusCode\": $STATUS,
        \"responseTime\": $LATENCY
      }" > /dev/null &
    
    COUNT=$((COUNT+1))
  done
  sleep 1
done

wait

echo "✅ Load test complete!"
echo "Total logs sent: $COUNT"
echo "Check Grafana at http://localhost:3000 for metrics"
