#!/bin/bash

echo "======================================"
echo "Load Testing Log Producer System"
echo "======================================"
echo ""

# Number of concurrent requests
CONCURRENT=50
# Total requests
REQUESTS=10000

echo "Configuration:"
echo "  - Concurrent: $CONCURRENT"
echo "  - Total Requests: $REQUESTS"
echo ""

# Create test payload
cat > /tmp/log-payload.json << 'PAYLOAD'
{
  "source": "load-test",
  "level": "INFO",
  "message": "Load test message with some data",
  "metadata": {
    "test": "true",
    "iteration": "1"
  }
}
PAYLOAD

echo "Starting load test..."
echo ""

# Run Apache Bench if available, otherwise use curl loop
if command -v ab &> /dev/null; then
    ab -n $REQUESTS -c $CONCURRENT \
       -p /tmp/log-payload.json \
       -T application/json \
       http://localhost:8080/api/logs
else
    echo "Apache Bench not found, using curl..."
    
    START=$(date +%s)
    
    for i in $(seq 1 $REQUESTS); do
        curl -X POST http://localhost:8080/api/logs \
          -H "Content-Type: application/json" \
          -d @/tmp/log-payload.json \
          -s -o /dev/null &
        
        if [ $((i % CONCURRENT)) -eq 0 ]; then
            wait
        fi
        
        if [ $((i % 1000)) -eq 0 ]; then
            echo "Sent $i requests..."
        fi
    done
    
    wait
    END=$(date +%s)
    DURATION=$((END - START))
    RPS=$((REQUESTS / DURATION))
    
    echo ""
    echo "Results:"
    echo "  - Total Time: ${DURATION}s"
    echo "  - Requests/sec: $RPS"
fi

rm /tmp/log-payload.json

echo ""
echo "Check metrics at:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000"
