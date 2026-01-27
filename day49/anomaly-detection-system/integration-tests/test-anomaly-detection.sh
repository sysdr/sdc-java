#!/bin/bash

echo "=== Anomaly Detection Integration Tests ==="

API_URL="http://localhost:8080/api"

echo "Waiting for services to be ready..."
sleep 30

echo "1. Testing API Gateway health..."
curl -s http://localhost:8080/actuator/health | jq .

echo -e "\n2. Checking recent anomalies..."
curl -s "$API_URL/anomalies/recent?hours=1" | jq 'length'

echo -e "\n3. Getting anomaly statistics..."
curl -s "$API_URL/anomalies/stats" | jq .

echo -e "\n4. Checking high confidence anomalies..."
curl -s "$API_URL/anomalies/high-confidence?minConfidence=0.7" | jq 'length'

echo -e "\n5. Monitoring anomaly detection for 60 seconds..."
for i in {1..12}; do
    count=$(curl -s "$API_URL/anomalies/recent?hours=1" | jq 'length')
    echo "Iteration $i: $count anomalies detected"
    sleep 5
done

echo -e "\n=== Integration Tests Complete ==="
