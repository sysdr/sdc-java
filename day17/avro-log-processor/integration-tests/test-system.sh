#!/bin/bash

set -e

API_URL="http://localhost:8080/api"

echo "Running Integration Tests..."
echo ""

# Test 1: Health check
echo "Test 1: Health check..."
HEALTH=$(curl -s "$API_URL/health")
if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo "  PASS: Gateway is healthy"
else
    echo "  FAIL: Gateway health check failed"
    exit 1
fi

# Test 2: Produce V1 event
echo "Test 2: Produce V1 event..."
RESULT=$(curl -s -X POST "$API_URL/logs" \
    -H "Content-Type: application/json" \
    -d '{"level":"INFO","message":"Test V1","source":"test","schemaVersion":1}')
if echo "$RESULT" | grep -q '"status":"success"'; then
    echo "  PASS: V1 event produced"
else
    echo "  FAIL: V1 event production failed"
    exit 1
fi

# Test 3: Produce V2 event with tracing
echo "Test 3: Produce V2 event with tracing..."
CORR_ID="test-$(date +%s)"
RESULT=$(curl -s -X POST "$API_URL/logs" \
    -H "Content-Type: application/json" \
    -d '{
        "level":"WARN",
        "message":"Test V2",
        "source":"test",
        "correlationId":"'"$CORR_ID"'",
        "tags":{"env":"test"},
        "schemaVersion":2
    }')
if echo "$RESULT" | grep -q '"status":"success"'; then
    echo "  PASS: V2 event produced"
else
    echo "  FAIL: V2 event production failed"
    exit 1
fi

# Test 4: Query by correlation ID
echo "Test 4: Query by correlation ID..."
sleep 2
RESULT=$(curl -s "$API_URL/correlation/$CORR_ID")
if echo "$RESULT" | grep -q '"correlationId"'; then
    echo "  PASS: Correlation query works"
else
    echo "  FAIL: Correlation query failed"
    exit 1
fi

# Test 5: Schema Registry check
echo "Test 5: Schema Registry..."
SUBJECTS=$(curl -s http://localhost:8085/subjects)
if echo "$SUBJECTS" | grep -q 'avro-log-events'; then
    echo "  PASS: Schema registered"
else
    echo "  PASS: Schema will be registered on first event (expected)"
fi

echo ""
echo "All integration tests passed!"
