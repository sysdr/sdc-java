#!/bin/bash

set -e

echo "==================================="
echo "Integration Tests"
echo "==================================="

BASE_URL="http://localhost"

echo ""
echo "1. Testing log producer health..."
curl -f "${BASE_URL}:8081/api/logs/health" || exit 1
echo "✓ Log producer healthy"

echo ""
echo "2. Testing indexing service health..."
curl -f "${BASE_URL}:8082/api/index/health" || exit 1
echo "✓ Indexing service healthy"

echo ""
echo "3. Testing search API health..."
curl -f "${BASE_URL}:8083/api/search/health" || exit 1
echo "✓ Search API healthy"

echo ""
echo "4. Generating test log events..."
curl -X POST "${BASE_URL}:8081/api/logs/generate?count=1000" || exit 1
echo "✓ Generated 1000 log events"

echo ""
echo "5. Waiting for indexing to complete..."
sleep 10

echo ""
echo "6. Checking index stats..."
curl -f "${BASE_URL}:8082/api/index/stats" || exit 1
echo "✓ Index stats retrieved"

echo ""
echo "7. Testing search for 'error authentication'..."
SEARCH_RESULT=$(curl -s "${BASE_URL}:8083/api/search?query=error%20authentication&limit=10")
echo "$SEARCH_RESULT" | head -20
echo "✓ Search completed"

echo ""
echo "==================================="
echo "All integration tests passed!"
echo "==================================="
