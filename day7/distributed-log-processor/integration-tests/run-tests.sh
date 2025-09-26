#!/bin/bash

set -e

echo "🧪 Running integration tests..."

BASE_URL_PRODUCER="http://localhost:8081/producer"
BASE_URL_GATEWAY="http://localhost:8080/gateway"

# Test 1: Health checks
echo "Test 1: Health checks"
curl -f "$BASE_URL_PRODUCER/api/logs/health" | grep -q "healthy" && echo "✅ Producer health OK" || echo "❌ Producer health FAILED"
curl -f "$BASE_URL_GATEWAY/api/query/health" | grep -q "healthy" && echo "✅ Gateway health OK" || echo "❌ Gateway health FAILED"

# Test 2: Log ingestion
echo "Test 2: Log ingestion"
LOG_ID=$(uuidgen)
curl -X POST "$BASE_URL_PRODUCER/api/logs/ingest" \
  -H "Content-Type: application/json" \
  -d "{
    \"id\": \"$LOG_ID\",
    \"level\": \"INFO\",
    \"message\": \"Integration test log message\",
    \"source\": \"integration-test\",
    \"metadata\": {\"testType\": \"integration\"}
  }" | grep -q "accepted" && echo "✅ Log ingestion OK" || echo "❌ Log ingestion FAILED"

# Wait for processing
echo "Waiting for log processing..."
sleep 5

# Test 3: Log query
echo "Test 3: Log query"
curl -f "$BASE_URL_GATEWAY/api/query/logs?source=integration-test" | grep -q "$LOG_ID" && echo "✅ Log query OK" || echo "❌ Log query FAILED"

# Test 4: Batch ingestion
echo "Test 4: Batch ingestion"
curl -X POST "$BASE_URL_PRODUCER/api/logs/batch" \
  -H "Content-Type: application/json" \
  -d "[
    {\"id\": \"$(uuidgen)\", \"level\": \"ERROR\", \"message\": \"Batch test 1\", \"source\": \"batch-test\"},
    {\"id\": \"$(uuidgen)\", \"level\": \"WARN\", \"message\": \"Batch test 2\", \"source\": \"batch-test\"}
  ]" | grep -q "completed" && echo "✅ Batch ingestion OK" || echo "❌ Batch ingestion FAILED"

# Test 5: Statistics
echo "Test 5: Statistics"
curl -f "$BASE_URL_GATEWAY/api/query/stats" | grep -q "totalLogs" && echo "✅ Statistics OK" || echo "❌ Statistics FAILED"

echo "🎉 Integration tests completed!"
