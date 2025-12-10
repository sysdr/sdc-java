#!/bin/bash

echo "🔥 Starting Load Test for Cluster System..."

GATEWAY_URL="http://localhost:8080"
COORDINATOR_URL="http://localhost:8081"
DURATION=60
CONCURRENT_REQUESTS=10

echo "Duration: ${DURATION}s"
echo "Concurrent requests: ${CONCURRENT_REQUESTS}"
echo ""

# Monitor cluster health during load test
monitor_cluster() {
    while true; do
        status=$(curl -s $COORDINATOR_URL/cluster/status)
        healthy=$(echo "$status" | jq '.healthyNodes')
        total=$(echo "$status" | jq '.totalNodes')
        echo "[$(date +%H:%M:%S)] Cluster: $healthy/$total healthy nodes"
        sleep 5
    done
}

# Start monitoring in background
monitor_cluster &
MONITOR_PID=$!

# Give monitoring a second to start
sleep 1

echo "Load test running for ${DURATION} seconds..."
echo "Monitor cluster health at: $COORDINATOR_URL/cluster/status"

# Simulate load (adjust based on your actual endpoints)
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    (
        end=$((SECONDS+$DURATION))
        while [ $SECONDS -lt $end ]; do
            curl -s -o /dev/null $GATEWAY_URL/actuator/health
            sleep 0.1
        done
    ) &
done

# Wait for load test to complete
sleep $DURATION

# Stop monitoring
kill $MONITOR_PID 2>/dev/null

echo ""
echo "✅ Load test completed!"
echo "Check metrics at: http://localhost:9090 (Prometheus)"
echo "Check dashboards at: http://localhost:3000 (Grafana, admin/admin)"
