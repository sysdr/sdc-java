#!/bin/bash

# Integration test script for log processing system
set -e

echo "🧪 Running system integration tests..."

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 30

# Test API Gateway health
echo "🔍 Testing API Gateway health..."
curl -f http://localhost:8090/actuator/health || {
    echo "❌ API Gateway health check failed"
    exit 1
}

# Test Log Generator health
echo "🔍 Testing Log Generator health..."
curl -f http://localhost:8080/actuator/health || {
    echo "❌ Log Generator health check failed"
    exit 1
}

# Start log generation
echo "🚀 Starting log generation..."
curl -X POST http://localhost:8090/api/v1/generator/start || {
    echo "❌ Failed to start log generation"
    exit 1
}

# Wait for some events to be generated
echo "⏳ Waiting for log generation (30 seconds)..."
sleep 30

# Check generation status
echo "📊 Checking generation status..."
STATUS_RESPONSE=$(curl -s http://localhost:8090/api/v1/generator/status)
echo "Status response: $STATUS_RESPONSE"

# Verify events are being generated
TOTAL_GENERATED=$(echo $STATUS_RESPONSE | jq -r '.totalGenerated')
if [ "$TOTAL_GENERATED" -gt 0 ]; then
    echo "✅ Log generation is working! Generated: $TOTAL_GENERATED events"
else
    echo "❌ No events generated"
    exit 1
fi

# Stop generation
echo "🛑 Stopping log generation..."
curl -X POST http://localhost:8090/api/v1/generator/stop

# Test Prometheus metrics
echo "📈 Testing Prometheus metrics..."
curl -f http://localhost:8080/actuator/prometheus | grep -q "log_events_generated" || {
    echo "❌ Prometheus metrics not available"
    exit 1
}

echo "✅ All integration tests passed!"
