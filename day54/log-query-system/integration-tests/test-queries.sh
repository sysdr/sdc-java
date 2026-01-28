#!/bin/bash

BASE_URL="http://localhost:8080"

echo "🧪 Running integration tests..."

# Test 1: Simple SELECT query
echo "Test 1: Simple SELECT"
curl -X POST "$BASE_URL/api/query" \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT * WHERE level = '\''ERROR'\'' LIMIT 10"}' \
  | jq .

sleep 2

# Test 2: Query with multiple conditions
echo "Test 2: Multiple conditions"
curl -X POST "$BASE_URL/api/query" \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT service, level WHERE level = '\''ERROR'\'' AND service = '\''api-gateway'\''"}' \
  | jq .

sleep 2

# Test 3: Check cache hit
echo "Test 3: Cache hit (repeat query)"
curl -X POST "$BASE_URL/api/query" \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT service, level WHERE level = '\''ERROR'\'' AND service = '\''api-gateway'\''"}' \
  | jq .

echo "✅ Integration tests complete"
