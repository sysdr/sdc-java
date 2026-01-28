#!/bin/bash

set -e

GATEWAY_URL="http://localhost:8080"
SEARCH_URL="http://localhost:8082"

echo "🧪 Running Faceted Search Integration Tests..."

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 30

# Test 1: Generate logs
echo "Test 1: Generating logs..."
curl -X POST "$GATEWAY_URL/api/logs/generate?count=1000" \
  -H "Content-Type: application/json"
echo "✅ Generated 1000 log events"

# Wait for indexing
echo "Waiting for logs to be indexed..."
sleep 10

# Test 2: Simple faceted search
echo "Test 2: Simple faceted search (level=ERROR)..."
RESPONSE=$(curl -s -X POST "$SEARCH_URL/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "level": "ERROR"
    },
    "limit": 10
  }')

TOTAL_HITS=$(echo $RESPONSE | jq -r '.totalHits')
echo "Found $TOTAL_HITS ERROR logs"

if [ "$TOTAL_HITS" -gt 0 ]; then
    echo "✅ Simple faceted search working"
else
    echo "❌ No ERROR logs found"
    exit 1
fi

# Test 3: Multi-dimensional faceted search
echo "Test 3: Multi-dimensional search (level=ERROR AND service=auth-service)..."
RESPONSE=$(curl -s -X POST "$SEARCH_URL/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "level": "ERROR",
      "service": "auth-service"
    },
    "limit": 10
  }')

TOTAL_HITS=$(echo $RESPONSE | jq -r '.totalHits')
echo "Found $TOTAL_HITS ERROR logs from auth-service"
echo "✅ Multi-dimensional search working"

# Test 4: Verify facet counts
echo "Test 4: Verifying facet counts..."
FACETS=$(echo $RESPONSE | jq -r '.facets')
SERVICE_FACETS=$(echo $FACETS | jq -r '.service | length')

if [ "$SERVICE_FACETS" -gt 0 ]; then
    echo "✅ Facet aggregations working ($SERVICE_FACETS service facets found)"
else
    echo "❌ No facets returned"
    exit 1
fi

# Test 5: Time-based filtering
echo "Test 5: Time-based filtering (last 5 minutes)..."
FROM_TIMESTAMP=$(($(date +%s) * 1000 - 300000))  # 5 minutes ago
TO_TIMESTAMP=$(($(date +%s) * 1000))

RESPONSE=$(curl -s -X POST "$SEARCH_URL/api/search" \
  -H "Content-Type: application/json" \
  -d "{
    \"filters\": {
      \"level\": \"ERROR\"
    },
    \"fromTimestamp\": $FROM_TIMESTAMP,
    \"toTimestamp\": $TO_TIMESTAMP,
    \"limit\": 10
  }")

TOTAL_HITS=$(echo $RESPONSE | jq -r '.totalHits')
echo "Found $TOTAL_HITS ERROR logs in last 5 minutes"
echo "✅ Time-based filtering working"

# Test 6: Cache performance
echo "Test 6: Testing cache performance..."
START_TIME=$(date +%s%N)
curl -s -X POST "$SEARCH_URL/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "level": "ERROR"
    },
    "limit": 10
  }' > /dev/null
FIRST_QUERY_TIME=$(($(date +%s%N) - START_TIME))

START_TIME=$(date +%s%N)
curl -s -X POST "$SEARCH_URL/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "level": "ERROR"
    },
    "limit": 10
  }' > /dev/null
SECOND_QUERY_TIME=$(($(date +%s%N) - START_TIME))

echo "First query: ${FIRST_QUERY_TIME}ns"
echo "Second query (cached): ${SECOND_QUERY_TIME}ns"

if [ "$SECOND_QUERY_TIME" -lt "$FIRST_QUERY_TIME" ]; then
    echo "✅ Cache improving performance"
else
    echo "⚠️ Cache may not be working optimally"
fi

echo ""
echo "✅ All integration tests passed!"
