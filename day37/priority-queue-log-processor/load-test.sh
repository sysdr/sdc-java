#!/bin/bash

echo "🔥 Running Priority Queue Load Test..."
echo "Sending 1000 log events to test priority routing..."

for i in {1..1000}; do
  # Generate different priority logs
  if [ $((i % 10)) -eq 0 ]; then
    # 10% critical logs
    curl -s -X POST http://localhost:8081/api/logs \
      -H "Content-Type: application/json" \
      -d "{\"message\":\"OutOfMemoryError in JVM\",\"level\":\"ERROR\",\"service\":\"payment-service\",\"httpStatus\":500}" \
      > /dev/null
  elif [ $((i % 5)) -eq 0 ]; then
    # 20% high priority logs
    curl -s -X POST http://localhost:8081/api/logs \
      -H "Content-Type: application/json" \
      -d "{\"message\":\"Payment failed\",\"level\":\"ERROR\",\"service\":\"payment-service\",\"httpStatus\":400,\"latencyMs\":1500}" \
      > /dev/null
  else
    # 70% normal/low priority logs
    curl -s -X POST http://localhost:8081/api/logs \
      -H "Content-Type: application/json" \
      -d "{\"message\":\"Request processed\",\"level\":\"INFO\",\"service\":\"api-gateway\",\"httpStatus\":200,\"latencyMs\":50}" \
      > /dev/null
  fi
  
  if [ $((i % 100)) -eq 0 ]; then
    echo "Sent $i events..."
  fi
done

echo "✅ Load test completed!"
echo "📊 Check stats: http://localhost:8080/api/stats"
