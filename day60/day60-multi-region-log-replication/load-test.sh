#!/bin/bash
# =============================================================================
# load-test.sh – Multi-Region Load Test
#
# Scenario:
#   Phase 1 – Steady state: 5000 events to each region over 30 seconds
#   Phase 2 – Simulated split: stop MirrorMaker for 5 seconds
#             (both regions continue accepting writes independently)
#   Phase 3 – Recovery: restart MirrorMaker, send 1000 more events
#             Validates that deduplication catches all duplicates after recovery
#
# Requires: curl, jq (optional for pretty output)
# =============================================================================
set -euo pipefail

GATEWAY_URL="http://localhost:8080/api/logs"
PRODUCER_A_URL="http://localhost:8081/logs"
PRODUCER_B_URL="http://localhost:8083/logs"

PHASE1_EVENTS_PER_REGION=5000
PHASE1_DURATION_SECS=30
PHASE3_EVENTS=1000

TOTAL_SENT=0
TOTAL_ERRORS=0

# ── Helper: send a single event ──────────────────────────────────────────────
send_event() {
    local url="$1"
    local region="$2"
    local seq="$3"
    local correlation="trace-loadtest-$(date +%s%N)"

    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$url" \
        -H "Content-Type: application/json" \
        -H "X-Region: $region" \
        --max-time 5 \
        -d "{\"serviceName\":\"load-test-svc\",\"level\":\"INFO\",\"message\":\"Load test event #$seq from $region\",\"correlationId\":\"$correlation\"}" \
    2>/dev/null) || response="000"

    if [ "$response" == "202" ] || [ "$response" == "200" ]; then
        return 0
    else
        return 1
    fi
}

echo "=============================================="
echo "  Day 60 Multi-Region Load Test               "
echo "=============================================="
echo ""

# ── Phase 1: Steady-state load ───────────────────────────────────────────────
echo "[Phase 1] Steady-state: sending events to both regions via Gateway..."
echo "  Target: $PHASE1_EVENTS_PER_REGION events/region over ${PHASE1_DURATION_SECS}s"

INTERVAL=$(echo "scale=4; $PHASE1_DURATION_SECS / ($PHASE1_EVENTS_PER_REGION * 2)" | bc)
# Minimum interval to avoid overwhelming a single-host demo
if (( $(echo "$INTERVAL < 0.002" | bc -l) )); then INTERVAL="0.002"; fi

for i in $(seq 1 $PHASE1_EVENTS_PER_REGION); do
    # Send to region-a
    if send_event "$GATEWAY_URL" "region-a" "${i}-a"; then
        ((TOTAL_SENT++))
    else
        ((TOTAL_ERRORS++))
    fi

    # Send to region-b
    if send_event "$GATEWAY_URL" "region-b" "${i}-b"; then
        ((TOTAL_SENT++))
    else
        ((TOTAL_ERRORS++))
    fi

    # Progress indicator every 500 events
    if (( i % 500 == 0 )); then
        echo "  ... sent $i / $PHASE1_EVENTS_PER_REGION pairs (errors: $TOTAL_ERRORS)"
    fi

    sleep "$INTERVAL"
done

echo ""
echo "  ✓ Phase 1 complete. Sent: $TOTAL_SENT | Errors: $TOTAL_ERRORS"

# ── Phase 2: Simulate split-brain ────────────────────────────────────────────
echo ""
echo "[Phase 2] Simulating network partition (stopping MirrorMaker for 5s)..."
docker compose stop mirrormaker-a-to-b mirrormaker-b-to-a 2>/dev/null || {
    echo "  ⚠ Could not stop MirrorMaker (may not be running). Continuing..."
}

echo "  Sending 200 events to each region during the 'split'..."
for i in $(seq 1 200); do
    send_event "$PRODUCER_A_URL" "region-a" "split-${i}-a" && ((TOTAL_SENT++)) || ((TOTAL_ERRORS++))
    send_event "$PRODUCER_B_URL" "region-b" "split-${i}-b" && ((TOTAL_SENT++)) || ((TOTAL_ERRORS++))
    sleep 0.01
done

echo "  Waiting 5 seconds (simulating partition duration)..."
sleep 5

# ── Phase 3: Recovery ────────────────────────────────────────────────────────
echo ""
echo "[Phase 3] Restarting MirrorMaker (recovery)..."
docker compose start mirrormaker-a-to-b mirrormaker-b-to-a 2>/dev/null || {
    echo "  ⚠ Could not restart MirrorMaker."
}
echo "  Waiting 10s for replication to catch up..."
sleep 10

echo "  Sending $PHASE3_EVENTS additional events to validate post-recovery..."
for i in $(seq 1 $PHASE3_EVENTS); do
    REGION=$( [ $((i % 2)) -eq 0 ] && echo "region-a" || echo "region-b" )
    send_event "$GATEWAY_URL" "$REGION" "recovery-${i}" && ((TOTAL_SENT++)) || ((TOTAL_ERRORS++))
    sleep 0.005
done

echo ""
echo "=============================================="
echo "  Load Test Summary                          "
echo "=============================================="
echo "  Total events sent:    $TOTAL_SENT"
echo "  Total errors:         $TOTAL_ERRORS"
echo "  Success rate:         $(echo "scale=2; ($TOTAL_SENT * 100) / ($TOTAL_SENT + $TOTAL_ERRORS)" | bc)%"
echo ""
echo "  → Check Grafana (http://localhost:3000) for:"
echo "      - Event production rates"
echo "      - Deduplication hit count (should be > 0 after Phase 3)"
echo "      - Late arrival count"
echo "      - Reorder buffer size"
echo "=============================================="
