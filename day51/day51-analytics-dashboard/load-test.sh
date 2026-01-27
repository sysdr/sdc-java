#!/bin/bash

echo "🔥 Starting load test..."
echo "Generating 1000 requests per second for 60 seconds"
echo "Watch the dashboard at http://localhost:8080"
echo ""

END=$((SECONDS+60))

while [ $SECONDS -lt $END ]; do
    for i in {1..100}; do
        curl -s http://localhost:8080/api/metrics/health > /dev/null &
    done
    sleep 0.1
done

echo "✅ Load test complete!"
