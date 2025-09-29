#!/bin/bash

# Simplified load testing script for log processing system
set -e

echo "🔥 Starting simplified load test..."

# Configuration
DURATION=60
CONCURRENT_USERS=10
REQUESTS_PER_SECOND=100

# Function to make HTTP requests
make_request() {
    local url=$1
    local method=${2:-GET}
    
    if [ "$method" = "POST" ]; then
        curl -s -X POST "$url" -w "%{http_code}" -o /dev/null
    else
        curl -s "$url" -w "%{http_code}" -o /dev/null
    fi
}

# Test if services are running
echo "🔍 Checking service status..."
LOG_GEN_STATUS=$(make_request "http://localhost:8080/actuator/health" 2>/dev/null || echo "000")
API_GATEWAY_STATUS=$(make_request "http://localhost:8090/actuator/health" 2>/dev/null || echo "000")

echo "Log Generator (8080): HTTP $LOG_GEN_STATUS"
echo "API Gateway (8090): HTTP $API_GATEWAY_STATUS"

# Try to start log generation
echo "🚀 Attempting to start log generation..."
START_RESPONSE=$(make_request "http://localhost:8090/api/v1/generator/start" "POST")
echo "Start response: HTTP $START_RESPONSE"

# If direct API fails, try alternative endpoints
if [ "$START_RESPONSE" != "200" ] && [ "$START_RESPONSE" != "201" ]; then
    echo "🔄 Trying alternative endpoints..."
    
    # Try log generator directly
    START_RESPONSE=$(make_request "http://localhost:8080/api/v1/generator/start" "POST")
    echo "Direct log generator start: HTTP $START_RESPONSE"
fi

# Simple load test using curl in a loop
echo "📊 Running simple load test..."
echo "Duration: ${DURATION}s, Target: ${REQUESTS_PER_SECOND} requests"

START_TIME=$(date +%s)
REQUEST_COUNT=0
SUCCESS_COUNT=0
ERROR_COUNT=0

while [ $(($(date +%s) - START_TIME)) -lt $DURATION ]; do
    # Make requests to status endpoint
    RESPONSE=$(make_request "http://localhost:8090/api/v1/generator/status" 2>/dev/null || echo "000")
    
    REQUEST_COUNT=$((REQUEST_COUNT + 1))
    
    if [ "$RESPONSE" = "200" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        ERROR_COUNT=$((ERROR_COUNT + 1))
    fi
    
    # Show progress every 10 requests
    if [ $((REQUEST_COUNT % 10)) -eq 0 ]; then
        ELAPSED=$(($(date +%s) - START_TIME))
        CURRENT_RPS=$((REQUEST_COUNT / (ELAPSED + 1)))
        echo "[${ELAPSED}s] Requests: $REQUEST_COUNT, Success: $SUCCESS_COUNT, Errors: $ERROR_COUNT, RPS: $CURRENT_RPS"
    fi
    
    # Control request rate (roughly)
    sleep 0.01
done

# Final results
ELAPSED=$(($(date +%s) - START_TIME))
FINAL_RPS=$((REQUEST_COUNT / ELAPSED))

echo ""
echo "✅ Load test completed!"
echo "📊 Final Results:"
echo "   Total Requests: $REQUEST_COUNT"
echo "   Successful: $SUCCESS_COUNT"
echo "   Errors: $ERROR_COUNT"
echo "   Duration: ${ELAPSED}s"
echo "   Average RPS: $FINAL_RPS"
echo "   Success Rate: $((SUCCESS_COUNT * 100 / REQUEST_COUNT))%"

# Try to get final status
echo ""
echo "🔍 Final status check..."
FINAL_STATUS=$(make_request "http://localhost:8090/api/v1/generator/status" 2>/dev/null || echo "000")
echo "Final API status: HTTP $FINAL_STATUS"

echo "🔍 Check Grafana dashboard at http://localhost:3000 for detailed metrics"
