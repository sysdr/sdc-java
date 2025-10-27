#!/bin/bash

echo "🧪 Running Integration Tests for JSON Log Processing System"
echo "============================================"

BASE_URL="http://localhost:8080"
PRODUCER_URL="http://localhost:8081"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test counters
PASSED=0
FAILED=0

# Test function
test_endpoint() {
    local test_name=$1
    local url=$2
    local method=$3
    local data=$4
    local expected_code=$5
    
    echo -n "Testing: $test_name... "
    
    if [ "$method" == "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url")
    else
        response=$(curl -s -w "\n%{http_code}" "$url")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq "$expected_code" ]; then
        echo -e "${GREEN}✓ PASSED${NC} (HTTP $http_code)"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗ FAILED${NC} (Expected $expected_code, got $http_code)"
        echo "Response: $body"
        ((FAILED++))
        return 1
    fi
}

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Test 1: Health Check
test_endpoint "API Gateway Health" "$BASE_URL/api/logs/health" "GET" "" 200

# Test 2: Valid Log Ingestion
valid_log='{
  "level": "INFO",
  "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
  "message": "Test log message",
  "service": "integration-test",
  "metadata": {
    "test": true,
    "version": "1.0"
  }
}'
test_endpoint "Valid Log Ingestion" "$BASE_URL/api/logs/ingest" "POST" "$valid_log" 202

# Test 3: Invalid Log (missing required field)
invalid_log='{
  "level": "INFO",
  "message": "Test without timestamp",
  "service": "integration-test"
}'
test_endpoint "Invalid Log (Schema Violation)" "$BASE_URL/api/logs/ingest" "POST" "$invalid_log" 400

# Test 4: Invalid Log Level
invalid_level='{
  "level": "INVALID_LEVEL",
  "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
  "message": "Test log",
  "service": "integration-test"
}'
test_endpoint "Invalid Log Level" "$BASE_URL/api/logs/ingest" "POST" "$invalid_level" 400

# Test 5: Batch Ingestion
batch_logs='[
  {
    "level": "INFO",
    "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
    "message": "Batch log 1",
    "service": "batch-test"
  },
  {
    "level": "WARN",
    "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
    "message": "Batch log 2",
    "service": "batch-test"
  }
]'
test_endpoint "Batch Log Ingestion" "$BASE_URL/api/logs/ingest/batch" "POST" "$batch_logs" 200

# Test 6: High Volume Test
echo ""
echo "🚀 Running high-volume test (100 logs)..."
success_count=0
for i in {1..100}; do
    log='{
      "level": "INFO",
      "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")'",
      "message": "High volume test log '$i'",
      "service": "load-test"
    }'
    
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$log" \
        "$BASE_URL/api/logs/ingest")
    
    if [ "$http_code" -eq 202 ]; then
        ((success_count++))
    fi
    
    # Show progress every 20 logs
    if [ $((i % 20)) -eq 0 ]; then
        echo "  Progress: $i/100 logs sent"
    fi
done

echo -e "High volume test: $success_count/100 logs accepted"
if [ $success_count -ge 95 ]; then
    echo -e "${GREEN}✓ PASSED${NC} (>95% success rate)"
    ((PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} (<95% success rate)"
    ((FAILED++))
fi

# Summary
echo ""
echo "============================================"
echo "Test Summary:"
echo -e "  ${GREEN}Passed: $PASSED${NC}"
echo -e "  ${RED}Failed: $FAILED${NC}"
echo "============================================"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed! ✓${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed ✗${NC}"
    exit 1
fi
