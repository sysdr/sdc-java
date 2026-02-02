#!/bin/bash

echo "=== Load Test: Log Search System ==="

PRODUCER_URL="http://localhost:8081/api/logs/generate"
DURATION=60
RATE=10

echo "Generating logs at $RATE logs/sec for $DURATION seconds..."

end=$((SECONDS+DURATION))
while [ $SECONDS -lt $end ]; do
    for i in $(seq 1 $RATE); do
        curl -s -X POST "$PRODUCER_URL" > /dev/null &
    done
    sleep 1
done

wait

echo -e "\nLoad test complete. Check Grafana at http://localhost:3000"
echo "Username: admin, Password: admin"
