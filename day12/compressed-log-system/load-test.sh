#!/bin/bash

echo "🔥 Starting load test for compressed log system..."

GATEWAY_URL="http://localhost:8081/api/logs"
DURATION=60
REQUESTS_PER_SECOND=10

echo "Sending $REQUESTS_PER_SECOND requests/second for $DURATION seconds"

END=$((SECONDS+DURATION))

while [ $SECONDS -lt $END ]; do
    for i in $(seq 1 $REQUESTS_PER_SECOND); do
        curl -X POST $GATEWAY_URL \
            -H "Content-Type: application/json" \
            -d "{
                \"level\": \"INFO\",
                \"service\": \"load-test\",
                \"message\": \"Load test message with repeated data data data data\",
                \"user_id\": \"user_$RANDOM\",
                \"request_id\": \"req_$RANDOM\"
            }" \
            --silent --output /dev/null &
    done
    sleep 1
done

wait

echo "✅ Load test complete!"
echo "📊 Check metrics at:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000"
