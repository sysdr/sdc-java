#!/bin/bash

set -e

echo "==================================="
echo "Load Testing Log Processing System"
echo "==================================="
echo ""

BASE_URL="http://localhost:8081"
DURATION=60

# Function to check if service is running
check_service() {
    if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
        echo "Error: Producer service is not running at $BASE_URL"
        echo "Please start the services first with ./start-services.sh"
        exit 1
    fi
}

# Reset statistics
reset_stats() {
    echo "Resetting statistics..."
    curl -s -X POST "$BASE_URL/api/load/reset" | jq .
    echo ""
}

# Get current statistics
get_stats() {
    curl -s "$BASE_URL/api/load/stats" | jq .
}

# Run load test
run_load_test() {
    local duration=$1
    echo "Starting $duration second load test..."
    echo ""
    
    # Start the load generation
    curl -s -X POST "$BASE_URL/api/load/burst?durationSeconds=$duration" | jq .
    
    echo ""
    echo "Load test running... Monitoring throughput:"
    echo ""
    
    # Monitor progress
    for i in $(seq 1 $duration); do
        sleep 1
        stats=$(get_stats)
        total=$(echo $stats | jq -r '.totalGenerated')
        rate=$((total / i))
        echo "  Time: ${i}s | Total: $total | Rate: $rate logs/sec"
    done
}

# Display results
display_results() {
    echo ""
    echo "==================================="
    echo "Load Test Results"
    echo "==================================="
    
    stats=$(get_stats)
    echo "$stats" | jq .
    
    total=$(echo "$stats" | jq -r '.totalGenerated')
    avg_rate=$((total / DURATION))
    
    echo ""
    echo "Summary:"
    echo "  Duration: ${DURATION}s"
    echo "  Total Logs: $total"
    echo "  Average Rate: $avg_rate logs/sec"
    echo ""
    
    echo "Fetching latency metrics from consumer..."
    consumer_metrics=$(curl -s "http://localhost:8082/actuator/metrics/log.processing.time")
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "Processing Latency:"
        echo "$consumer_metrics" | jq '.measurements[] | select(.statistic | contains("PERCENTILE")) | {percentile: .statistic, value: .value}'
    fi
    
    echo ""
    echo "View detailed metrics at:"
    echo "  - Prometheus: http://localhost:9090"
    echo "  - Grafana: http://localhost:3000"
    echo "  - Gateway Metrics: http://localhost:8080/api/metrics/summary"
}

# Main execution
main() {
    check_service
    reset_stats
    sleep 2
    run_load_test $DURATION
    sleep 5
    display_results
}

main
