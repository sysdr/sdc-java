#!/bin/bash

echo "🎯 Running Demo to Generate Metrics"
echo "===================================="

API_URL="http://localhost:8080"

# Send a large batch of ERROR logs to trigger alerts
echo "Sending 200 ERROR logs to trigger error threshold alert (>100 in 5 min)..."
for i in {1..200}; do
  curl -s -X POST ${API_URL}/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"service\": \"demo-service\",
      \"level\": \"ERROR\",
      \"message\": \"Demo error message $i\",
      \"statusCode\": 500
    }" > /dev/null
done
echo "✅ Sent 200 ERROR logs"

# Send high-latency logs
echo "Sending 60 high-latency logs (>2s)..."
for i in {1..60}; do
  curl -s -X POST ${API_URL}/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"service\": \"demo-service\",
      \"level\": \"INFO\",
      \"message\": \"Slow request $i\",
      \"responseTime\": 3000
    }" > /dev/null
done
echo "✅ Sent 60 high-latency logs"

# Send 5xx errors
echo "Sending 30 5xx server errors..."
for i in {1..30}; do
  curl -s -X POST ${API_URL}/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"service\": \"demo-service\",
      \"level\": \"ERROR\",
      \"message\": \"Server error $i\",
      \"statusCode\": 503
    }" > /dev/null
done
echo "✅ Sent 30 5xx error logs"

echo ""
echo "✅ Demo complete! Waiting 15 seconds for processing..."
sleep 15

echo ""
echo "📊 Current Metrics:"
echo "==================="
echo "Logs Ingested:"
curl -s ${API_URL}/actuator/prometheus | grep "logs_ingested_total" || echo "No metrics yet"

echo ""
echo "Check Prometheus: http://localhost:9090"
echo "Check Grafana: http://localhost:3000 (admin/admin)"
