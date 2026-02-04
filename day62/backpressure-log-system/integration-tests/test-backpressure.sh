#!/bin/bash

echo "Testing backpressure mechanisms..."

# Send normal load
echo "Sending 100 requests..."
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"correlationId\":\"test-$i\",\"severity\":\"INFO\",\"message\":\"Test message\",\"source\":\"integration-test\",\"timestamp\":$(date +%s)000}" \
    -w "\n" &
done

wait

# Check health
sleep 2
echo ""
echo "Checking system health..."
curl http://localhost:8080/api/logs/health | jq .

echo ""
echo "Sending high load (500 requests)..."
for i in {1..500}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"correlationId\":\"load-$i\",\"severity\":\"DEBUG\",\"message\":\"Load test\",\"source\":\"integration-test\",\"timestamp\":$(date +%s)000}" \
    -s -o /dev/null -w "%{http_code}\n" &
done | sort | uniq -c

wait

echo ""
echo "Final health check..."
curl http://localhost:8080/api/logs/health | jq .
