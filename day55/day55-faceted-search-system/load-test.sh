#!/bin/bash

set -e

SEARCH_URL="http://localhost:8082/api/search"
CONCURRENT_REQUESTS=50
TOTAL_REQUESTS=1000

echo "⚡ Running Load Test on Faceted Search..."
echo "Target: $SEARCH_URL"
echo "Concurrent requests: $CONCURRENT_REQUESTS"
echo "Total requests: $TOTAL_REQUESTS"
echo ""

# Generate test data first
echo "Generating test data..."
curl -X POST "http://localhost:8080/api/logs/generate?count=10000" \
  -H "Content-Type: application/json"
sleep 15

# Run load test with Apache Bench
echo "Starting load test..."

ab -n $TOTAL_REQUESTS -c $CONCURRENT_REQUESTS \
   -p - -T 'application/json' \
   "$SEARCH_URL" << 'REQUEST'
{
  "filters": {
    "level": "ERROR"
  },
  "limit": 10
}
REQUEST

echo ""
echo "✅ Load test completed!"
echo ""
echo "Test different query patterns:"
echo "1. Multi-facet: service=auth-service AND level=ERROR"
echo "2. Time-based: Last 15 minutes"
echo "3. High-cardinality: All services with counts"
