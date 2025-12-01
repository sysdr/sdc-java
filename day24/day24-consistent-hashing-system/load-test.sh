#!/bin/bash

echo "🔥 Running load test with 10,000 logs"

GATEWAY_URL="http://localhost:8080/api/logs"

# Generate diverse source IPs for distribution testing
for i in {1..10000}; do
  SOURCE_IP="192.168.$((RANDOM % 256)).$((RANDOM % 256))"
  LEVEL=("INFO" "WARN" "ERROR" "DEBUG")
  LEVEL_INDEX=$((RANDOM % 4))
  
  curl -s -X POST "$GATEWAY_URL" \
    -H "Content-Type: application/json" \
    -d "{
      \"message\": \"Test log message $i\",
      \"level\": \"${LEVEL[$LEVEL_INDEX]}\",
      \"source\": \"load-test\",
      \"sourceIp\": \"$SOURCE_IP\",
      \"application\": \"test-app\",
      \"environment\": \"production\"
    }" > /dev/null
  
  if [ $((i % 1000)) -eq 0 ]; then
    echo "Sent $i logs..."
  fi
done

echo "✅ Load test complete!"
echo ""
echo "View distribution metrics:"
echo "curl http://localhost:8081/api/coordinator/metrics/distribution | jq"
