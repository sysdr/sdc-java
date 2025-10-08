#!/bin/bash

set -e

GATEWAY_URL="http://localhost:8080/api/logs/ship"
CONCURRENT_USERS=${1:-10}
REQUESTS_PER_USER=${2:-100}
TOTAL_REQUESTS=$((CONCURRENT_USERS * REQUESTS_PER_USER))

echo "🔥 UDP Log Processing Load Test"
echo "================================"
echo "Gateway URL: $GATEWAY_URL"
echo "Concurrent Users: $CONCURRENT_USERS"
echo "Requests per User: $REQUESTS_PER_USER"
echo "Total Requests: $TOTAL_REQUESTS"
echo ""

# Function to send requests
send_requests() {
    user_id=$1
    for i in $(seq 1 $REQUESTS_PER_USER); do
        curl -s -X POST $GATEWAY_URL \
          -H "Content-Type: application/json" \
          -d "{
            \"source\": \"load-test-user-${user_id}\",
            \"level\": \"INFO\",
            \"message\": \"Load test message ${i} from user ${user_id}\"
          }" > /dev/null
    done
}

# Start time
start_time=$(date +%s)

echo "Starting load test..."

# Launch concurrent users
for user in $(seq 1 $CONCURRENT_USERS); do
    send_requests $user &
done

# Wait for all background jobs
wait

# End time
end_time=$(date +%s)
duration=$((end_time - start_time))
throughput=$((TOTAL_REQUESTS / duration))

echo ""
echo "✅ Load test completed!"
echo "Duration: ${duration}s"
echo "Throughput: ${throughput} requests/second"
echo ""
echo "📊 View metrics at:"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo "- Producer metrics: http://localhost:8081/actuator/prometheus"
echo "- Consumer metrics: http://localhost:8082/actuator/prometheus"
