#!/bin/bash
# =============================================================================
# Integration Test Suite – Day 60
#
# Validates end-to-end behavior of the multi-region replication system.
# Assumes the full stack is running (run ./setup.sh first).
# =============================================================================
set -euo pipefail

PASS=0
FAIL=0
GATEWAY="http://localhost:8080/api/logs"
PRODUCER_A="http://localhost:8081/logs"

assert_status() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    if [ "$expected" == "$actual" ]; then
        echo "  ✓ PASS: $test_name"
        ((PASS++))
    else
        echo "  ✗ FAIL: $test_name (expected=$expected actual=$actual)"
        ((FAIL++))
    fi
}

echo "=============================================="
echo "  Day 60 Integration Tests                   "
echo "=============================================="
echo ""

# ── Test 1: Gateway accepts event for region-a ──────────────────────────────
echo "[Test 1] Gateway routes event to Region A"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY" \
    -H "Content-Type: application/json" \
    -H "X-Region: region-a" \
    -d '{"serviceName":"int-test","level":"INFO","message":"Test 1","correlationId":"int-trace-001"}')
assert_status "Gateway -> Region A (202)" "202" "$STATUS"

# ── Test 2: Gateway accepts event for region-b ──────────────────────────────
echo ""
echo "[Test 2] Gateway routes event to Region B"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY" \
    -H "Content-Type: application/json" \
    -H "X-Region: region-b" \
    -d '{"serviceName":"int-test","level":"WARN","message":"Test 2","correlationId":"int-trace-002"}')
assert_status "Gateway -> Region B (202)" "202" "$STATUS"

# ── Test 3: Direct producer access ──────────────────────────────────────────
echo ""
echo "[Test 3] Direct producer health check"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$PRODUCER_A/health")
assert_status "Producer A health (200)" "200" "$STATUS"

# ── Test 4: Batch endpoint ───────────────────────────────────────────────────
echo ""
echo "[Test 4] Batch produce endpoint"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$PRODUCER_A/batch" \
    -H "Content-Type: application/json" \
    -d '[
        {"serviceName":"batch-svc","level":"DEBUG","message":"Batch item 1","correlationId":"batch-trace-1"},
        {"serviceName":"batch-svc","level":"INFO","message":"Batch item 2","correlationId":"batch-trace-2"},
        {"serviceName":"batch-svc","level":"WARN","message":"Batch item 3","correlationId":"batch-trace-3"}
    ]')
assert_status "Batch produce (202)" "202" "$STATUS"

# ── Test 5: Empty batch rejection ───────────────────────────────────────────
echo ""
echo "[Test 5] Empty batch is rejected"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$PRODUCER_A/batch" \
    -H "Content-Type: application/json" \
    -d '[]')
assert_status "Empty batch rejected (400)" "400" "$STATUS"

# ── Test 6: Actuator metrics endpoint available ─────────────────────────────
echo ""
echo "[Test 6] Actuator metrics endpoint"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8081/actuator/prometheus")
assert_status "Producer A /actuator/prometheus (200)" "200" "$STATUS"

# ── Test 7: Consumer actuator ────────────────────────────────────────────────
echo ""
echo "[Test 7] Consumer actuator health"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8082/actuator/health")
assert_status "Consumer A health (200)" "200" "$STATUS"

# ── Test 8: Duplicate event detection (send same correlationId twice) ────────
echo ""
echo "[Test 8] Duplicate event via same correlationId (functional check)"
# Send the same event twice — the second should be deduplicated by the consumer
# We can't assert the dedup counter here directly, but we verify both POSTs succeed
# (the dedup happens async in the consumer)
curl -s -o /dev/null -X POST "$PRODUCER_A" \
    -H "Content-Type: application/json" \
    -d '{"serviceName":"dedup-test","level":"INFO","message":"Dedup test","correlationId":"dedup-trace-999"}'
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$PRODUCER_A" \
    -H "Content-Type: application/json" \
    -d '{"serviceName":"dedup-test","level":"INFO","message":"Dedup test duplicate","correlationId":"dedup-trace-999"}')
assert_status "Second event with same correlation accepted by producer (202)" "202" "$STATUS"
echo "  ℹ Note: Consumer-side dedup is verified via Grafana dedup_hits counter"

echo ""
echo "=============================================="
echo "  Test Results: $PASS passed, $FAIL failed   "
echo "=============================================="

if [ $FAIL -gt 0 ]; then
    echo "  SOME TESTS FAILED"
    exit 1
else
    echo "  ALL TESTS PASSED"
    exit 0
fi
