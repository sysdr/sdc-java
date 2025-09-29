#!/bin/bash

# Load testing script for log processing system
set -e

echo "🔥 Starting load test..."

# Configuration
DURATION=60
CONCURRENT_USERS=10
REQUESTS_PER_SECOND=100

# Start log generation if not already running
echo "🚀 Starting log generation..."
curl -X POST http://localhost:8090/api/v1/generator/start

# Run load test using Apache Bench
echo "📊 Running load test with Apache Bench..."
echo "Duration: ${DURATION}s, Concurrent users: ${CONCURRENT_USERS}, Target RPS: ${REQUESTS_PER_SECOND}"

# Test status endpoint
ab -t $DURATION -c $CONCURRENT_USERS -k http://localhost:8090/api/v1/generator/status

# Monitor metrics during load test
echo "📈 Collecting metrics..."
for i in {1..12}; do
    STATUS=$(curl -s http://localhost:8090/api/v1/generator/status)
    CURRENT_RATE=$(echo $STATUS | jq -r '.currentRate')
    TOTAL_GENERATED=$(echo $STATUS | jq -r '.totalGenerated')
    RATE_LIMITED=$(echo $STATUS | jq -r '.totalRateLimited')
    
    echo "[$i/12] Rate: ${CURRENT_RATE}/s, Total: ${TOTAL_GENERATED}, Rate Limited: ${RATE_LIMITED}"
    sleep 5
done

echo "✅ Load test completed!"
echo "🔍 Check Grafana dashboard at http://localhost:3000 for detailed metrics"
