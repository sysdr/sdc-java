#!/bin/bash

echo "🔥 Starting load test for Log Processing System..."

API_BASE="http://localhost:8081/api/logs"
DURATION=60
CONCURRENT_REQUESTS=5

echo "📊 Generating load for ${DURATION} seconds with ${CONCURRENT_REQUESTS} concurrent requests..."

# Function to send log entries
send_logs() {
    local id=$1
    local end_time=$((SECONDS + DURATION))
    
    while [ $SECONDS -lt $end_time ]; do
        # Send custom Apache log
        curl -s -X POST "$API_BASE/send" \
            -H "Content-Type: text/plain" \
            -d "192.168.1.$((RANDOM % 254 + 1)) - - [$(date '+%d/%b/%Y:%H:%M:%S %z')] \"GET /api/test HTTP/1.1\" 200 1234" \
            > /dev/null
        
        # Send custom Nginx log  
        curl -s -X POST "$API_BASE/send" \
            -H "Content-Type: text/plain" \
            -d "10.0.0.$((RANDOM % 254 + 1)) - - [$(date '+%d/%b/%Y:%H:%M:%S %z')] \"POST /api/data HTTP/1.1\" 201 567 \"-\" \"curl/7.68.0\" rt=0.123" \
            > /dev/null
        
        sleep 0.1
    done
    
    echo "✅ Worker $id completed"
}

# Start concurrent workers
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    send_logs $i &
done

# Wait for all workers to complete
wait

echo ""
echo "✅ Load test completed!"
echo "📈 Check metrics at:"
echo "  • Prometheus: http://localhost:9090"
echo "  • Grafana: http://localhost:3000"
echo "  • API Results: http://localhost:8080/api/logs"
echo ""
