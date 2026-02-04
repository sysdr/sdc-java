#!/bin/bash
# =============================================================================
# load-test.sh — Generate synthetic log traffic against the API Gateway
# =============================================================================
# Modes:
#   ./load-test.sh              → Normal: 100 requests, 10 concurrent
#   ./load-test.sh --burst      → Burst:  500 requests, 50 concurrent (stress)
#   ./load-test.sh --levels     → Send events at all log levels
# =============================================================================
set -euo pipefail

GATEWAY_URL="http://localhost:8080/api/logs"

MODE="${1:---normal}"
SOURCES=("auth-service" "payment-service" "user-service" "order-service" "notification-service")
LEVELS=("DEBUG" "INFO" "WARN" "ERROR" "FATAL")
MESSAGES=(
    "Request processed successfully"
    "Database connection pool exhausted"
    "Retry attempt 3 of 5"
    "Cache miss on key user:12345"
    "Upstream timeout after 3000ms"
    "Health check passed"
    "Circuit breaker tripped for downstream"
    "Partition rebalance detected"
)

generate_event() {
    local source="${SOURCES[$((RANDOM % ${#SOURCES[@]}))]}"
    local level="${1:-${LEVELS[$((RANDOM % ${#LEVELS[@]}))]}}"
    local message="${MESSAGES[$((RANDOM % ${#MESSAGES[@]}))]}"

    cat <<PAYLOAD
{
  "source": "${source}",
  "level": "${level}",
  "message": "${message}"
}
PAYLOAD
}

run_request() {
    local event=$(generate_event "$1")
    local response
    response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL" \
        -H "Content-Type: application/json" \
        -d "$event")
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    echo "$http_code"
}

echo "=============================================="
echo " Day 61 — Load Test"
echo " Target: $GATEWAY_URL"
echo "=============================================="

case "$MODE" in
    --normal)
        TOTAL=100
        CONCURRENT=10
        echo " Mode: Normal (${TOTAL} requests, ${CONCURRENT} concurrent)"
        ;;
    --burst)
        TOTAL=500
        CONCURRENT=50
        echo " Mode: Burst (${TOTAL} requests, ${CONCURRENT} concurrent)"
        ;;
    --levels)
        TOTAL=50
        CONCURRENT=10
        echo " Mode: All Levels (${TOTAL} requests across all log levels)"
        ;;
    *)
        echo "Usage: $0 [--normal|--burst|--levels]"
        exit 1
        ;;
esac
echo "=============================================="
echo ""

# Pre-flight check
echo "[*] Checking gateway availability..."
if ! curl -sf "http://localhost:8080/api/logs/health" > /dev/null; then
    echo "ERROR: Gateway is not available. Run setup.sh first."
    exit 1
fi
echo "    Gateway is up."
echo ""

# Counters
SUCCESS=0
FAIL=0
TOTAL_TIME_START=$(date +%s%N)

# Fire requests
echo "[*] Sending ${TOTAL} requests..."
for i in $(seq 1 $TOTAL); do
    # Control concurrency with background jobs
    if (( i % CONCURRENT == 0 )); then
        wait
    fi

    (
        if [ "$MODE" = "--levels" ]; then
            LEVEL="${LEVELS[$((i % ${#LEVELS[@]}))]}";
            code=$(run_request "$LEVEL")
        else
            code=$(run_request "")
        fi

        if [ "$code" = "200" ] || [ "$code" = "202" ]; then
            echo "OK"
        else
            echo "FAIL:${code}"
        fi
    ) &
done
wait

TOTAL_TIME_END=$(date +%s%N)
ELAPSED_MS=$(( (TOTAL_TIME_END - TOTAL_TIME_START) / 1000000 ))

echo ""
echo "=============================================="
echo " Load Test Complete"
echo "=============================================="
echo "  Total requests: ${TOTAL}"
echo "  Elapsed time:   ${ELAPSED_MS}ms"
echo "  Throughput:     $(( TOTAL * 1000 / ELAPSED_MS )) req/s"
echo ""
echo "  Check Grafana for real-time metrics:"
echo "  http://localhost:3000"
echo "=============================================="
