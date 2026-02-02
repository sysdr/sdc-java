#!/bin/bash

echo "🔥 Running Load Test on Search API"
echo "===================================="

API_KEY="${API_KEY:-standard-test-key}"
BASE_URL="http://localhost:8080/api/v1/logs"

echo "Test 1: Simple search query"
curl -X GET "${BASE_URL}/search?service=checkout&level=ERROR&timeRange=1h&limit=10" \
     -H "X-API-Key: ${API_KEY}" \
     -w "\nStatus: %{http_code}\nTime: %{time_total}s\n\n"

echo "Test 2: Wildcard search"
curl -X GET "${BASE_URL}/search?service=payment&message=timeout*&timeRange=6h&limit=50" \
     -H "X-API-Key: ${API_KEY}" \
     -w "\nStatus: %{http_code}\nTime: %{time_total}s\n\n"

echo "Test 3: Large result set with pagination"
RESPONSE=$(curl -s -X GET "${BASE_URL}/search?service=checkout&timeRange=24h&limit=100" \
     -H "X-API-Key: ${API_KEY}")

echo "$RESPONSE" | jq .

CURSOR=$(echo "$RESPONSE" | jq -r '.nextCursor')

if [ "$CURSOR" != "null" ]; then
    echo "Test 4: Fetch next page using cursor"
    curl -X GET "${BASE_URL}/search?service=checkout&timeRange=24h&limit=100&cursor=${CURSOR}" \
         -H "X-API-Key: ${API_KEY}" \
         -w "\nStatus: %{http_code}\nTime: %{time_total}s\n\n"
fi

echo "Test 5: Rate limiting (send 20 requests rapidly)"
RATE_LIMIT_KEY="${RATE_LIMIT_KEY:-rate-limit-test}"
for i in {1..20}; do
    curl -s -X GET "${BASE_URL}/search?service=test&limit=1" \
         -H "X-API-Key: ${RATE_LIMIT_KEY}" \
         -w "Request $i: %{http_code}\n" \
         -o /dev/null
done

echo ""
echo "Load test complete! Check Grafana at http://localhost:3000 for metrics"
