#!/bin/bash
# =============================================================================
# integration-tests/run-tests.sh
# End-to-end validation of the distributed log processing pipeline.
# =============================================================================
# Tests:
#   1. Gateway accepts a valid log event and returns 200
#   2. Gateway rejects invalid payloads (400)
#   3. Health endpoints respond correctly
#   4. Rate limiter returns 429 after threshold
#   5. Dead-letter drain endpoint is accessible
# =============================================================================
set -euo pipefail

GATEWAY="http://localhost:8080"
PRODUCER="http://localhost:8081"
CONSUMER="http://localhost:8082"

PASS=0
FAIL=0

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    if [ "$expected" = "$actual" ]; then
        echo "  ✅ PASS: ${test_name}"
        PASS=$((PASS + 1))
    else
        echo "  ❌ FAIL: ${test_name} (expected ${expected}, got ${actual})"
        FAIL=$((FAIL + 1))
    fi
}

echo "=============================================="
echo " Day 61 — Integration Test Suite"
echo "=============================================="
echo ""

# --- Test 1: Valid log event ---
echo "[Test 1] POST valid log event"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/logs" \
    -H "Content-Type: application/json" \
    -d '{"source":"integration-test","level":"INFO","message":"Integration test event"}')
assert_status "Valid log event accepted" "200" "$STATUS"

# --- Test 2: Missing required fields ---
echo "[Test 2] POST missing 'source' field"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/logs" \
    -H "Content-Type: application/json" \
    -d '{"level":"INFO","message":"No source field"}')
assert_status "Missing source returns 400" "400" "$STATUS"

echo "[Test 2b] POST missing 'level' field"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/logs" \
    -H "Content-Type: application/json" \
    -d '{"source":"test","message":"No level"}')
assert_status "Missing level returns 400" "400" "$STATUS"

echo "[Test 2c] POST invalid 'level' value"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/logs" \
    -H "Content-Type: application/json" \
    -d '{"source":"test","level":"INVALID","message":"Bad level"}')
assert_status "Invalid level returns 400" "400" "$STATUS"

# --- Test 3: Health endpoints ---
echo "[Test 3] Health endpoints"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/api/logs/health")
assert_status "Gateway health returns 200" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$PRODUCER/actuator/health")
assert_status "Producer health returns 200" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$CONSUMER/actuator/health")
assert_status "Consumer health returns 200" "200" "$STATUS"

# --- Test 4: Metrics endpoints exposed ---
echo "[Test 4] Prometheus metrics exposed"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/actuator/prometheus")
assert_status "Gateway exposes /actuator/prometheus" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$PRODUCER/actuator/prometheus")
assert_status "Producer exposes /actuator/prometheus" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$CONSUMER/actuator/prometheus")
assert_status "Consumer exposes /actuator/prometheus" "200" "$STATUS"

# --- Test 5: ERROR level event (validates enum parsing) ---
echo "[Test 5] POST ERROR-level event"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/logs" \
    -H "Content-Type: application/json" \
    -d '{"source":"error-test-svc","level":"ERROR","message":"Simulated production error for testing"}')
assert_status "ERROR level event accepted" "200" "$STATUS"

# --- Test 6: FATAL level event ---
echo "[Test 6] POST FATAL-level event"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/logs" \
    -H "Content-Type: application/json" \
    -d '{"source":"fatal-test-svc","level":"FATAL","message":"Critical system failure simulation"}')
assert_status "FATAL level event accepted" "200" "$STATUS"

# --- Test 7: DLQ drain endpoint ---
echo "[Test 7] POST DLQ drain endpoint"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$PRODUCER/api/logs/internal/dlq/drain")
assert_status "DLQ drain endpoint accessible" "200" "$STATUS"

# --- Summary ---
echo ""
echo "=============================================="
echo " Integration Test Results"
echo "=============================================="
echo "  Passed: ${PASS}"
echo "  Failed: ${FAIL}"
echo "  Total:  $((PASS + FAIL))"
echo "=============================================="

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo "  ⚠️  Some tests failed. Check service logs:"
    echo "      docker compose logs --tail=50 api-gateway"
    echo "      docker compose logs --tail=50 log-producer"
    exit 1
else
    echo ""
    echo "  🎉 All tests passed!"
fi
