#!/bin/bash

echo "🔥 Starting load test..."

SOURCES=("payment-service" "user-service" "order-service" "inventory-service" "notification-service")
LEVELS=("INFO" "WARN" "ERROR")

for i in {1..10000}; do
  SOURCE=${SOURCES[$RANDOM % ${#SOURCES[@]}]}
  LEVEL=${LEVELS[$RANDOM % ${#LEVELS[@]}]}
  
  curl -s -X POST http://localhost:8081/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"source\": \"$SOURCE\",
      \"message\": \"Test log message $i\",
      \"level\": \"$LEVEL\"
    }" > /dev/null
  
  if [ $((i % 1000)) -eq 0 ]; then
    echo "Sent $i logs..."
  fi
done

echo "✅ Load test complete: 10,000 logs sent"
echo ""
echo "Query test:"
curl -X POST http://localhost:8084/api/query/logs \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2024-11-28T00:00:00",
    "endTime": "2024-11-29T00:00:00",
    "source": "payment-service",
    "limit": 10
  }' | jq .
