#!/bin/bash

echo "=== Anomaly Detection Load Test ==="

API_URL="http://localhost:8080/api"
DURATION=60
CONCURRENT=10

echo "Running load test for ${DURATION} seconds with ${CONCURRENT} concurrent requests..."

# Install Apache Bench if not available
if ! command -v ab &> /dev/null; then
    echo "Apache Bench (ab) not found. Install it with: apt-get install apache2-utils"
    exit 1
fi

# Test API Gateway
echo -e "\n1. Testing anomaly stats endpoint..."
ab -n 1000 -c $CONCURRENT -t $DURATION "$API_URL/anomalies/stats"

echo -e "\n2. Testing recent anomalies endpoint..."
ab -n 1000 -c $CONCURRENT -t $DURATION "$API_URL/anomalies/recent"

echo -e "\n3. Final statistics..."
curl -s "$API_URL/anomalies/stats" | jq .

echo -e "\n=== Load Test Complete ==="
