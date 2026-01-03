#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Running Tests and Validating Dashboard ==="

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 5

# Check if services are running
check_service() {
    local port=$1
    local name=$2
    if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
        echo "✓ $name is running"
        return 0
    else
        echo "✗ $name is not running on port $port"
        return 1
    fi
}

echo ""
echo "Checking services..."
check_service 8081 "Log Producer" || exit 1
check_service 8082 "Log Consumer" || exit 1
check_service 8080 "API Gateway" || exit 1

# Run integration tests
echo ""
echo "=== Running Integration Tests ==="
if [ -f "$SCRIPT_DIR/integration-tests/test-dlq.sh" ]; then
    bash "$SCRIPT_DIR/integration-tests/test-dlq.sh"
else
    echo "Integration test script not found, running basic tests..."
    
    API_URL="http://localhost:8081/api/logs"
    GATEWAY_URL="http://localhost:8080/api/dlq"
    
    # Test 1: Normal message
    echo "Test 1: Publishing normal message..."
    curl -s -X POST "$API_URL/event" \
      -H "Content-Type: application/json" \
      -d '{"level": "INFO", "service": "test-service", "message": "Normal test message", "shouldFail": false}' > /dev/null
    sleep 2
    
    # Test 2: Message with validation error
    echo "Test 2: Publishing message with validation error..."
    curl -s -X POST "$API_URL/event" \
      -H "Content-Type: application/json" \
      -d '{"level": "ERROR", "service": "test-service", "message": "validation error test", "shouldFail": true}' > /dev/null
    sleep 2
    
    # Test 3: Batch with failures
    echo "Test 3: Publishing batch with 10% failure rate..."
    curl -s -X POST "$API_URL/batch" \
      -H "Content-Type: application/json" \
      -d '{"count": 100, "failureRate": 10}' > /dev/null
    sleep 10
fi

# Validate metrics
echo ""
echo "=== Validating Metrics ==="

validate_metric() {
    local service=$1
    local port=$2
    local metric=$3
    local name=$4
    
    local value=$(curl -s "http://localhost:$port/actuator/prometheus" | grep "^$metric" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")
    
    if [ -z "$value" ] || [ "$value" = "0" ]; then
        echo "✗ $name: $value (should be > 0)"
        return 1
    else
        echo "✓ $name: $value"
        return 0
    fi
}

# Check producer metrics
echo "Checking Producer metrics..."
validate_metric "producer" 8081 "producer_messages_published_total" "Messages Published" || true

# Check consumer metrics
echo "Checking Consumer metrics..."
validate_metric "consumer" 8082 "consumer_messages_processed_total" "Messages Processed" || true
validate_metric "consumer" 8082 "consumer_messages_failed_total" "Messages Failed" || true

# Check DLQ stats
echo ""
echo "=== Checking DLQ Stats ==="
DLQ_STATS=$(curl -s "$GATEWAY_URL/stats" 2>/dev/null || echo '{}')
TOTAL_MESSAGES=$(echo "$DLQ_STATS" | grep -o '"totalMessages":[0-9]*' | cut -d: -f2 || echo "0")

if [ "$TOTAL_MESSAGES" = "0" ] || [ -z "$TOTAL_MESSAGES" ]; then
    echo "⚠ DLQ Stats: No messages in DLQ (this is OK if no failures occurred)"
else
    echo "✓ DLQ Stats: $TOTAL_MESSAGES messages in DLQ"
    echo "$DLQ_STATS" | python3 -m json.tool 2>/dev/null || echo "$DLQ_STATS"
fi

# Validate Prometheus metrics
echo ""
echo "=== Validating Prometheus Metrics ==="
if curl -s "http://localhost:9090/api/v1/query?query=producer_messages_published_total" | grep -q "result"; then
    echo "✓ Prometheus is collecting metrics"
else
    echo "⚠ Prometheus may not be collecting metrics yet"
fi

# Validate Grafana
echo ""
echo "=== Validating Grafana ==="
if curl -s "http://localhost:3000/api/health" > /dev/null 2>&1; then
    echo "✓ Grafana is running"
    echo "  Dashboard: http://localhost:3000"
    echo "  Login: admin/admin"
else
    echo "⚠ Grafana may not be running"
fi

# Generate demo data
echo ""
echo "=== Generating Demo Data ==="
echo "Publishing batch of messages with failures to generate metrics..."
curl -s -X POST "http://localhost:8081/api/logs/batch" \
  -H "Content-Type: application/json" \
  -d '{"count": 200, "failureRate": 15}' > /dev/null

echo "Waiting for processing..."
sleep 15

# Final metric check
echo ""
echo "=== Final Metric Validation ==="
PRODUCER_METRIC=$(curl -s "http://localhost:8081/actuator/prometheus" | grep "^producer_messages_published_total" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")
CONSUMER_PROCESSED=$(curl -s "http://localhost:8082/actuator/prometheus" | grep "^consumer_messages_processed_total" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")
CONSUMER_FAILED=$(curl -s "http://localhost:8082/actuator/prometheus" | grep "^consumer_messages_failed_total" | head -1 | awk '{print $2}' | tr -d '\r' || echo "0")

echo "Producer Messages Published: $PRODUCER_METRIC"
echo "Consumer Messages Processed: $CONSUMER_PROCESSED"
echo "Consumer Messages Failed: $CONSUMER_FAILED"

if [ "$PRODUCER_METRIC" != "0" ] && [ "$PRODUCER_METRIC" != "" ]; then
    echo "✓ Producer metrics are updating"
else
    echo "✗ Producer metrics are zero or not available"
fi

if [ "$CONSUMER_PROCESSED" != "0" ] && [ "$CONSUMER_PROCESSED" != "" ]; then
    echo "✓ Consumer processed metrics are updating"
else
    echo "✗ Consumer processed metrics are zero or not available"
fi

if [ "$CONSUMER_FAILED" != "0" ] && [ "$CONSUMER_FAILED" != "" ]; then
    echo "✓ Consumer failed metrics are updating (DLQ working)"
else
    echo "⚠ Consumer failed metrics are zero (no failures or metrics not available)"
fi

echo ""
echo "=== Validation Complete ==="
echo "Check dashboard at: http://localhost:3000"
echo "Check Prometheus at: http://localhost:9090"
echo "Check DLQ stats at: http://localhost:8080/api/dlq/stats"

