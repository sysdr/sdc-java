#!/bin/bash

echo "Starting load test against monitoring dashboard..."
echo "This will generate sustained load for 60 seconds"

START_TIME=$(date +%s)
DURATION=60
COUNT=0

while [ $(($(date +%s) - START_TIME)) -lt $DURATION ]; do
    # Query various endpoints
    curl -s http://localhost:8082/api/metrics/requests/current > /dev/null &
    curl -s http://localhost:8082/api/metrics/errors > /dev/null &
    curl -s http://localhost:8082/api/metrics/regions > /dev/null &
    
    COUNT=$((COUNT + 3))
    
    if [ $((COUNT % 100)) -eq 0 ]; then
        echo "Sent $COUNT requests..."
    fi
    
    sleep 0.1
done

echo "Load test complete! Sent $COUNT total requests"
echo "Check Grafana dashboards at http://localhost:3000"
