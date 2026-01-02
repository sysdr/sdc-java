#!/bin/bash

echo "Running load test against routing service..."

# Number of requests
REQUESTS=1000
CONCURRENCY=10

echo "Sending $REQUESTS requests with concurrency $CONCURRENCY..."

# Generate test data
cat > /tmp/test-log.json << 'LOGEOF'
{
  "severity": "INFO",
  "source": "test-service",
  "type": "application",
  "message": "Load test log message"
}
LOGEOF

# Run Apache Bench if available
if command -v ab &> /dev/null; then
    ab -n $REQUESTS -c $CONCURRENCY -p /tmp/test-log.json \
       -T application/json \
       http://localhost:8081/api/logs
else
    echo "Apache Bench (ab) not found. Using curl loop..."
    for i in $(seq 1 $REQUESTS); do
        curl -s -X POST http://localhost:8081/api/logs \
          -H "Content-Type: application/json" \
          -d @/tmp/test-log.json > /dev/null &
        
        if [ $((i % CONCURRENCY)) -eq 0 ]; then
            wait
        fi
    done
    wait
fi

echo "Load test completed!"
rm /tmp/test-log.json
