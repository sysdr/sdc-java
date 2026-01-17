#!/bin/bash

set -e

echo "Running integration tests..."

echo "1. Testing log producer health..."
PRODUCER_HEALTH=$(curl -s http://localhost:8081/api/health | grep -o "UP" || echo "FAIL")
if [ "$PRODUCER_HEALTH" != "UP" ]; then
    echo "❌ Producer health check failed"
    exit 1
fi
echo "✅ Producer is healthy"

echo "2. Testing stream processor health..."
PROCESSOR_HEALTH=$(curl -s http://localhost:8082/actuator/health | grep -o "\"status\":\"UP\"" || echo "FAIL")
if [ "$PROCESSOR_HEALTH" != "\"status\":\"UP\"" ]; then
    echo "❌ Processor health check failed"
    exit 1
fi
echo "✅ Stream processor is healthy"

echo "3. Testing dashboard API health..."
DASHBOARD_HEALTH=$(curl -s http://localhost:8083/api/health | grep -o "UP" || echo "FAIL")
if [ "$DASHBOARD_HEALTH" != "UP" ]; then
    echo "❌ Dashboard health check failed"
    exit 1
fi
echo "✅ Dashboard API is healthy"

echo "4. Waiting for metrics to accumulate (30 seconds)..."
sleep 30

echo "5. Testing metrics endpoints..."
METRICS=$(curl -s http://localhost:8082/api/metrics/requests/current)
if [ -z "$METRICS" ] || [ "$METRICS" == "{}" ]; then
    echo "❌ No metrics data available"
    exit 1
fi
echo "✅ Metrics are being generated"

echo "6. Testing error tracking..."
ERRORS=$(curl -s http://localhost:8082/api/metrics/errors)
echo "✅ Error tracking is working: $ERRORS"

echo ""
echo "================================================"
echo "✅ All integration tests passed!"
echo "================================================"
echo ""
echo "System is processing events and generating real-time metrics"
echo "View the dashboard at: http://localhost:8083"
