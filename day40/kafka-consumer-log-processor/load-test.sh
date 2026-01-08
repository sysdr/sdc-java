#!/bin/bash

echo "🔥 Starting Load Test..."

LOG_LEVELS=("INFO" "WARN" "ERROR" "DEBUG")
SERVICES=("auth-service" "payment-service" "user-service" "notification-service")

send_log() {
  local level=${LOG_LEVELS[$RANDOM % ${#LOG_LEVELS[@]}]}
  local service=${SERVICES[$RANDOM % ${#SERVICES[@]}]}
  
  curl -s -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"level\": \"$level\",
      \"message\": \"Load test message $RANDOM\",
      \"service\": \"$service\",
      \"host\": \"load-test-host\",
      \"userId\": \"user-$RANDOM\",
      \"environment\": \"production\",
      \"version\": \"2.0.0\"
    }" > /dev/null
}

# Send logs in parallel
echo "Sending 1000 log events..."
for i in {1..1000}; do
  send_log &
  
  if (( i % 100 == 0 )); then
    echo "Sent $i logs..."
    wait
  fi
done

wait
echo "✅ Load test completed! Check Grafana for metrics."
