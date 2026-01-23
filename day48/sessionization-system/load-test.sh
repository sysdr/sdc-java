#!/bin/bash

echo "🔥 Running Load Tests on Sessionization System"
echo "=============================================="

BASE_URL="http://localhost:8080"

echo "Test 1: Concurrent session queries (100 requests)"
ab -n 100 -c 10 "$BASE_URL/api/sessions/active/user-1" > /dev/null 2>&1
echo "✅ Completed session query load test"

echo ""
echo "Test 2: Analytics stats queries (50 requests)"
ab -n 50 -c 5 "$BASE_URL/api/analytics/stats?hours=24" > /dev/null 2>&1
echo "✅ Completed analytics load test"

echo ""
echo "Test 3: Session history queries (50 requests)"
ab -n 50 -c 5 "$BASE_URL/api/sessions/history/user-1" > /dev/null 2>&1
echo "✅ Completed history load test"

echo ""
echo "📊 Checking system metrics:"
echo ""
echo "Events per second:"
curl -s http://localhost:9090/api/v1/query?query=rate\(kafka_producer_record_send_total\[1m\]\) | jq '.data.result[0].value[1]' || echo "N/A"

echo ""
echo "Active sessions:"
curl -s http://localhost:9090/api/v1/query?query=kafka_streams_state_store_records | jq '.data.result[0].value[1]' || echo "N/A"

echo ""
echo "✅ Load testing complete!"
