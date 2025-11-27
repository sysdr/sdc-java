#!/bin/bash

echo "🔥 Starting load test for enrichment pipeline..."

# Configuration
DURATION=60
RATE=100  # requests per second
ENDPOINT="http://localhost:8080/api/logs"

# Sample log event
generate_log() {
  local ip_addresses=("192.168.1.100" "192.168.1.101" "192.168.1.200" "192.168.1.201" "192.168.2.100")
  local services=("payment-service" "user-service" "order-service" "notification-service")
  local levels=("INFO" "WARN" "ERROR")
  
  local random_ip=${ip_addresses[$RANDOM % ${#ip_addresses[@]}]}
  local random_service=${services[$RANDOM % ${#services[@]}]}
  local random_level=${levels[$RANDOM % ${#levels[@]}]}
  
  cat <<JSON
{
  "level": "$random_level",
  "message": "Load test log message at $(date +%s)",
  "service": "$random_service",
  "source_ip": "$random_ip"
}
JSON
}

echo "Sending $RATE requests/second for $DURATION seconds..."
echo "Target throughput: $RATE logs/sec"

start_time=$(date +%s)
request_count=0

while [ $(($(date +%s) - start_time)) -lt $DURATION ]; do
  for i in $(seq 1 $RATE); do
    curl -s -X POST "$ENDPOINT" \
      -H "Content-Type: application/json" \
      -d "$(generate_log)" > /dev/null &
    
    request_count=$((request_count + 1))
  done
  sleep 1
done

wait

echo "✅ Load test complete!"
echo "Total requests sent: $request_count"
echo ""
echo "Check metrics:"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000"
echo ""
echo "Key metrics to review:"
echo "  enrichment_attempts_total"
echo "  enrichment_successes_total"
echo "  enrichment_latency_seconds"
echo "  enrichment_coverage"
echo "  resilience4j_circuitbreaker_state"
