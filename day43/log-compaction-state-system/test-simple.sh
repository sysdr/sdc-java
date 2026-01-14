#!/bin/bash

PRODUCER_URL="http://localhost:8081"
QUERY_URL="http://localhost:8083"

echo "Testing producer..."
RESPONSE=$(curl -s -X POST "$PRODUCER_URL/api/state/update" \
    -H "Content-Type: application/json" \
    -d '{"entityId":"final-test-1","entityType":"user","status":"active","attributes":{"test":"value"},"timestamp":"2026-01-14T07:30:00Z","version":1}')

echo "Producer response: $RESPONSE"

sleep 5

echo ""
echo "Checking metrics..."
curl -s http://localhost:8081/actuator/prometheus | grep "state_updates_total"

echo ""
echo "Checking query API..."
curl -s "$QUERY_URL/api/query/stats"

echo ""
echo "Checking consumer metrics..."
curl -s http://localhost:8082/actuator/prometheus | grep "state.materialization" | head -3
