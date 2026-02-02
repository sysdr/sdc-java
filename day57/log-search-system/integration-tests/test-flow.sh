#!/bin/bash

echo "=== Integration Test: Log Search System ==="

BASE_URL_PRODUCER="http://localhost:8081"
BASE_URL_SEARCH="http://localhost:8083"

echo "1. Checking service health..."
curl -s "$BASE_URL_PRODUCER/api/logs/health" | jq .
curl -s "$BASE_URL_SEARCH/api/search/health" | jq .

echo -e "\n2. Generating sample logs..."
for i in {1..10}; do
    curl -s -X POST "$BASE_URL_PRODUCER/api/logs/generate" | jq .
    sleep 0.5
done

echo -e "\n3. Waiting for indexing (5 seconds)..."
sleep 5

echo -e "\n4. Searching for 'timeout' logs..."
curl -s -X GET "$BASE_URL_SEARCH/api/search?q=timeout&size=5" | jq .

echo -e "\n5. Searching for ERROR logs..."
curl -s -X POST "$BASE_URL_SEARCH/api/search" \
    -H "Content-Type: application/json" \
    -d '{
        "query": "error",
        "severities": ["ERROR"],
        "size": 5
    }' | jq .

echo -e "\n=== Integration Test Complete ==="
