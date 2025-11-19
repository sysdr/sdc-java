#!/bin/bash

echo "Running integration tests..."

BASE_URL="http://localhost:8081/api/v1/normalize"
FAILED=0

# Test 1: JSON to JSON (passthrough)
echo -n "Test 1 - JSON passthrough: "
RESULT=$(curl -s -X POST "${BASE_URL}/json?targetFormat=JSON" \
    -H "Content-Type: application/json" \
    -d '{"level":"INFO","message":"test"}')
if echo "$RESULT" | grep -q '"success":true'; then
    echo "PASS"
else
    echo "FAIL"
    FAILED=$((FAILED+1))
fi

# Test 2: JSON to AVRO
echo -n "Test 2 - JSON to AVRO: "
RESULT=$(curl -s -X POST "${BASE_URL}/json?targetFormat=AVRO" \
    -H "Content-Type: application/json" \
    -d '{"id":"test-1","timestamp":1699999999999,"level":"INFO","service":"test","host":"localhost","message":"test message"}')
if echo "$RESULT" | grep -q '"success":true'; then
    echo "PASS"
else
    echo "FAIL"
    FAILED=$((FAILED+1))
fi

# Test 3: TEXT to JSON
echo -n "Test 3 - TEXT to JSON: "
RESULT=$(curl -s -X POST "${BASE_URL}/text?targetFormat=JSON" \
    -H "Content-Type: text/plain" \
    -d '2024-01-01T12:00:00Z [INFO] [my-service] Test message')
if echo "$RESULT" | grep -q '"success":true'; then
    echo "PASS"
else
    echo "FAIL"
    FAILED=$((FAILED+1))
fi

# Test 4: Get supported formats
echo -n "Test 4 - Get formats: "
RESULT=$(curl -s "${BASE_URL}/formats")
if echo "$RESULT" | grep -q 'JSON' && echo "$RESULT" | grep -q 'AVRO'; then
    echo "PASS"
else
    echo "FAIL"
    FAILED=$((FAILED+1))
fi

# Test 5: Batch normalization
echo -n "Test 5 - Batch normalization: "
RESULT=$(curl -s -X POST "${BASE_URL}/batch" \
    -H "Content-Type: application/json" \
    -d '{
        "logs": [
            {"id": "1", "data": "eyJsZXZlbCI6IklORk8iLCJtZXNzYWdlIjoidGVzdCJ9", "format": "JSON"}
        ],
        "targetFormat": "AVRO"
    }')
if echo "$RESULT" | grep -q '"successful":1'; then
    echo "PASS"
else
    echo "FAIL"
    FAILED=$((FAILED+1))
fi

echo ""
if [ $FAILED -eq 0 ]; then
    echo "All tests passed!"
    exit 0
else
    echo "$FAILED test(s) failed"
    exit 1
fi
