#!/bin/bash

API_URL="http://localhost:8080/api"

echo "Generating test events for dashboard..."

# Generate V1 events
for i in {1..10}; do
    curl -s -X POST "$API_URL/logs" \
        -H "Content-Type: application/json" \
        -d "{
            \"level\": \"INFO\",
            \"message\": \"Test event $i\",
            \"source\": \"dashboard-test\",
            \"schemaVersion\": 1
        }" > /dev/null
    sleep 0.2
done

echo "Generated 10 V1 events"

