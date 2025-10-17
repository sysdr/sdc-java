#!/bin/bash

echo "📊 Starting load test on TLS-secured system..."

GATEWAY_URL="https://localhost:8080/api/v1/logs"
DURATION=60
CONCURRENT=10

echo "Configuration:"
echo "  URL: ${GATEWAY_URL}"
echo "  Duration: ${DURATION}s"
echo "  Concurrent connections: ${CONCURRENT}"
echo ""

# Generate sample log payload
PAYLOAD='{"level":"INFO","message":"Load test event","source":"load-tester","metadata":{"timestamp":"'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"}}'

echo "Starting load test..."
echo ""

# Use Apache Bench if available
if command -v ab &> /dev/null; then
  ab -n 1000 -c ${CONCURRENT} -t ${DURATION} \
    -p <(echo ${PAYLOAD}) \
    -T "application/json" \
    -k ${GATEWAY_URL}
else
  echo "Apache Bench (ab) not found. Using curl in loop..."
  
  for i in {1..100}; do
    curl -k -X POST ${GATEWAY_URL} \
      -H "Content-Type: application/json" \
      -d "${PAYLOAD}" \
      -w "Request $i: HTTP %{http_code}, Time: %{time_total}s\n" \
      -o /dev/null -s &
    
    if [ $((i % CONCURRENT)) -eq 0 ]; then
      wait
    fi
  done
  
  wait
fi

echo ""
echo "✅ Load test complete!"
echo ""
echo "View metrics:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000"
