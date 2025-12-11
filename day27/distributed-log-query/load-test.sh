#!/bin/bash

echo "🔥 Running distributed query load test..."
echo "Waiting for services to be ready..."
sleep 10

COORDINATOR_URL="http://localhost:8080"

# Test 1: Time-range query
echo ""
echo "Test 1: Time-range query (last hour)"
START_TIME=$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)
END_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)

curl -X POST "$COORDINATOR_URL/api/query/logs" \
  -H "Content-Type: application/json" \
  -d "{
    \"startTime\": \"$START_TIME\",
    \"endTime\": \"$END_TIME\",
    \"limit\": 100
  }" | head -20

# Test 2: Service-specific query
echo ""
echo "Test 2: Service-specific query (payment-service)"
curl -X POST "$COORDINATOR_URL/api/query/logs" \
  -H "Content-Type: application/json" \
  -d "{
    \"startTime\": \"$START_TIME\",
    \"endTime\": \"$END_TIME\",
    \"serviceName\": \"payment-service\",
    \"limit\": 50
  }" | head -20

# Test 3: Log level query
echo ""
echo "Test 3: Error logs only"
curl -X POST "$COORDINATOR_URL/api/query/logs" \
  -H "Content-Type: application/json" \
  -d "{
    \"startTime\": \"$START_TIME\",
    \"endTime\": \"$END_TIME\",
    \"logLevel\": \"ERROR\",
    \"limit\": 50
  }" | head -20

# Test 4: Combined filters
echo ""
echo "Test 4: Combined filters (payment-service ERRORs)"
curl -X POST "$COORDINATOR_URL/api/query/logs" \
  -H "Content-Type: application/json" \
  -d "{
    \"startTime\": \"$START_TIME\",
    \"endTime\": \"$END_TIME\",
    \"serviceName\": \"payment-service\",
    \"logLevel\": \"ERROR\",
    \"limit\": 20
  }" | head -20

# Test 5: Query statistics
echo ""
echo "Test 5: Query statistics"
curl -X GET "$COORDINATOR_URL/api/query/stats" | jq .

echo ""
echo "✅ Load test complete!"
echo "📊 View metrics at: http://localhost:9090 (Prometheus)"
echo "📈 View dashboards at: http://localhost:3000 (Grafana, admin/admin)"
