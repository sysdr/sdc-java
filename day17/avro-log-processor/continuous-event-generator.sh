#!/bin/bash

API_URL="http://localhost:8080/api"
COUNTER=1

echo "Starting continuous event generator..."
echo "This will generate events every 3 seconds"
echo "Press Ctrl+C to stop"

while true; do
    # Generate a V1 event
    curl -s -X POST "$API_URL/logs" \
        -H "Content-Type: application/json" \
        -d "{
            \"level\": \"INFO\",
            \"message\": \"Continuous event #$COUNTER\",
            \"source\": \"continuous-generator\",
            \"schemaVersion\": 1
        }" > /dev/null 2>&1
    
    echo "[$(date +%H:%M:%S)] Generated event #$COUNTER"
    COUNTER=$((COUNTER + 1))
    sleep 3
done

