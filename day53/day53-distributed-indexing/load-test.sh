#!/bin/bash

set -e

echo "=== Distributed Indexing Load Test ==="

ROUTER_URL="http://localhost:8090"
COORDINATOR_URL="http://localhost:8080"
DURATION=60
CONCURRENCY=50

echo "Configuration:"
echo "  Router URL: $ROUTER_URL"
echo "  Coordinator URL: $COORDINATOR_URL"
echo "  Duration: ${DURATION}s"
echo "  Concurrency: $CONCURRENCY"
echo ""

# Generate test data file
echo "Generating test data..."
cat > /tmp/test-log.json << 'LOGEOF'
{
  "logId": "load_test_LOG_ID",
  "tenantId": "tenant_TENANT_ID",
  "timestamp": TIMESTAMP,
  "level": "INFO",
  "message": "Load test message LOG_ID with some searchable content",
  "service": "load-test-service"
}
LOGEOF

# Function to generate and send a log
send_log() {
  local id=$1
  local tenant=$((id % 10 + 1))
  local timestamp=$(date +%s)000
  
  sed -e "s/LOG_ID/$id/g" \
      -e "s/TENANT_ID/$tenant/g" \
      -e "s/TIMESTAMP/$timestamp/g" \
      /tmp/test-log.json | \
  curl -s -X POST "$ROUTER_URL/api/route" \
    -H "Content-Type: application/json" \
    -d @- > /dev/null
}

export -f send_log
export ROUTER_URL

# Write load test
echo "Starting write load test..."
echo "Sending logs for ${DURATION} seconds with concurrency ${CONCURRENCY}..."

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))
COUNTER=0

while [ $(date +%s) -lt $END_TIME ]; do
  for i in $(seq 1 $CONCURRENCY); do
    COUNTER=$((COUNTER + 1))
    send_log $COUNTER &
  done
  wait
done

TOTAL_TIME=$(($(date +%s) - START_TIME))
WRITE_THROUGHPUT=$((COUNTER / TOTAL_TIME))

echo ""
echo "Write Load Test Results:"
echo "  Total logs sent: $COUNTER"
echo "  Duration: ${TOTAL_TIME}s"
echo "  Throughput: ${WRITE_THROUGHPUT} logs/sec"

# Wait for indexing to complete
echo ""
echo "Waiting for indexing to complete..."
sleep 10

# Read load test
echo ""
echo "Starting read load test..."
echo "Executing ${CONCURRENCY} concurrent queries..."

QUERY_START=$(date +%s)
for i in $(seq 1 $CONCURRENCY); do
  curl -s "$COORDINATOR_URL/api/search?q=searchable&limit=100" > /dev/null &
done
wait
QUERY_DURATION=$(($(date +%s) - QUERY_START))

echo "Read Load Test Results:"
echo "  Queries executed: $CONCURRENCY"
echo "  Duration: ${QUERY_DURATION}s"
echo "  Throughput: $((CONCURRENCY / QUERY_DURATION)) queries/sec"

# Check final stats
echo ""
echo "Final Index Node Stats:"
for port in 8081 8082 8083; do
  echo "Node at port $port:"
  curl -s "http://localhost:$port/api/stats" | jq '{nodeId, numDocs, pendingDocs}'
done

# Check hash ring distribution
echo ""
echo "Hash Ring Distribution:"
curl -s "$ROUTER_URL/api/ring/distribution?samples=10000" | jq '.'

echo ""
echo "=== Load Test Completed ==="
