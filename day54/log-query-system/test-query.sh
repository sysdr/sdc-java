#!/bin/bash

BASE_URL="http://localhost:8080"

echo "Testing query execution..."

# Test query
curl -s -X POST "$BASE_URL/api/query" \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT * WHERE level = '\''ERROR'\'' LIMIT 10"}' | jq .

echo ""
echo "Checking metrics..."
curl -s "http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count" | jq '.data.result | length'
