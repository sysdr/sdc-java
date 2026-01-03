#!/bin/bash

API_URL="http://localhost:8081/api/logs/batch"
TOTAL_MESSAGES=10000
FAILURE_RATE=1  # 1% failure rate

echo "=== DLQ Load Test ==="
echo "Total Messages: $TOTAL_MESSAGES"
echo "Failure Rate: $FAILURE_RATE%"
echo "Expected Failures: $((TOTAL_MESSAGES * FAILURE_RATE / 100))"
echo ""

START_TIME=$(date +%s)

curl -s -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"count\": $TOTAL_MESSAGES,
    \"failureRate\": $FAILURE_RATE
  }" | jq .

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "Load test completed in $DURATION seconds"
echo "Throughput: $((TOTAL_MESSAGES / DURATION)) messages/second"
echo ""
echo "Check DLQ stats at: http://localhost:8080/api/dlq/stats"
echo "View Grafana dashboard: http://localhost:3000"
