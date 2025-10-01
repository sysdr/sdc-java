#!/bin/bash

set -e

echo "🧪 Running integration tests..."

# Test log ingestion
echo "📝 Testing log ingestion..."
TRACE_ID=$(curl -s -X POST http://localhost:8081/api/v1/logs \
  -H 'Content-Type: application/json' \
  -d '{"level":"INFO","source":"integration-test","message":"Test message for integration"}' \
  | grep -o 'X-Trace-Id: [^"]*' | cut -d' ' -f2 || echo "")

if [ -z "$TRACE_ID" ]; then
    echo "❌ Failed to ingest log"
    exit 1
fi

echo "✅ Log ingested with trace ID: $TRACE_ID"

# Wait for processing
sleep 5

# Test log retrieval
echo "🔍 Testing log retrieval..."
RESPONSE=$(curl -s http://localhost:8080/api/v1/query/logs/$TRACE_ID)

if [[ $RESPONSE == *"integration-test"* ]]; then
    echo "✅ Log retrieval successful"
else
    echo "❌ Log retrieval failed"
    exit 1
fi

# Test log query
echo "📊 Testing log query..."
QUERY_RESPONSE=$(curl -s "http://localhost:8080/api/v1/query/logs?source=integration-test&level=INFO")

if [[ $QUERY_RESPONSE == *"integration-test"* ]]; then
    echo "✅ Log query successful"
else
    echo "❌ Log query failed"
    exit 1
fi

# Test statistics
echo "📈 Testing statistics..."
STATS_RESPONSE=$(curl -s "http://localhost:8080/api/v1/query/logs/stats")

if [[ $STATS_RESPONSE == *"totalLogs"* ]]; then
    echo "✅ Statistics endpoint successful"
else
    echo "❌ Statistics endpoint failed"
    exit 1
fi

echo "🎉 All integration tests passed!"
