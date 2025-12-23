#!/bin/bash

echo "🔥 Starting Load Test..."

GATEWAY_URL="http://localhost:8080/api/v1/logs"
CONCURRENCY=50
TOTAL_REQUESTS=10000

echo "Configuration:"
echo "  Endpoint: $GATEWAY_URL"
echo "  Concurrency: $CONCURRENCY"
echo "  Total Requests: $TOTAL_REQUESTS"
echo ""

# Generate test payload
cat > /tmp/log-payload.json << 'PAYLOAD'
{
  "severity": "info",
  "category": "test",
  "message": "Load test message",
  "source": "load-tester"
}
PAYLOAD

# Run load test with Apache Bench
echo "Running Apache Bench load test..."
ab -n $TOTAL_REQUESTS -c $CONCURRENCY \
   -p /tmp/log-payload.json \
   -T "application/json" \
   "$GATEWAY_URL"

echo ""
echo "Check Grafana for metrics: http://localhost:3000"
echo "Check RabbitMQ Management: http://localhost:15672"
