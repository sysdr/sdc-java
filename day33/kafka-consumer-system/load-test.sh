#!/bin/bash

# Load test for consumer system - generates high-volume log traffic

echo "🚀 Starting Load Test for Consumer System..."
echo "⚠️ Make sure the producer service from Day 32 is running on port 8081"

DURATION=${1:-60}  # Default 60 seconds
RATE=${2:-1000}    # Default 1000 requests/sec

echo "📊 Test Parameters:"
echo "   Duration: ${DURATION}s"
echo "   Target Rate: ${RATE} logs/sec"
echo ""

# Function to send logs
send_log() {
    curl -s -X POST http://localhost:8081/api/logs \
        -H "Content-Type: application/json" \
        -d "{
            \"applicationName\": \"load-test-app\",
            \"level\": \"INFO\",
            \"message\": \"Load test message $(uuidgen)\",
            \"service\": \"load-test\",
            \"host\": \"loadtest-host-$((RANDOM % 10))\"
        }" > /dev/null
}

# Calculate requests per batch
BATCH_SIZE=10
BATCH_DELAY=$(echo "scale=4; $BATCH_SIZE / $RATE" | bc)

echo "🔥 Sending logs at ${RATE}/sec for ${DURATION} seconds..."
start_time=$(date +%s)
sent_count=0

while [ $(($(date +%s) - start_time)) -lt $DURATION ]; do
    for i in $(seq 1 $BATCH_SIZE); do
        send_log &
    done
    sent_count=$((sent_count + BATCH_SIZE))
    
    # Progress update every 5 seconds
    if [ $((sent_count % (RATE * 5))) -eq 0 ]; then
        elapsed=$(($(date +%s) - start_time))
        echo "⏱️ Sent $sent_count logs in ${elapsed}s ($(($sent_count / $elapsed)) logs/sec)"
    fi
    
    sleep $BATCH_DELAY
done

wait

echo ""
echo "✅ Load test completed!"
echo "📊 Total logs sent: $sent_count"
echo ""
echo "📈 Check consumer metrics:"
echo "   curl http://localhost:8080/api/monitoring/metrics/consumer | jq"
echo ""
echo "🔍 Check consumer lag in Prometheus:"
echo "   http://localhost:9090/graph?g0.expr=kafka_consumer_lag"
