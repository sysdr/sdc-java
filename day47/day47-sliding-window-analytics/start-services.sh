#!/bin/bash

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "🚀 Starting Sliding Window Analytics Services"
echo "=============================================="

# Kill any existing services
echo "Checking for existing services..."
pkill -f "spring-boot:run" || true
pkill -f "log-producer" || true
pkill -f "stream-processor" || true
pkill -f "query-api" || true
sleep 2

# Start log-producer
echo "Starting log-producer..."
cd "$PROJECT_ROOT/log-producer"
nohup mvn spring-boot:run > ../log-producer.log 2>&1 &
LOG_PRODUCER_PID=$!
echo "Log producer started (PID: $LOG_PRODUCER_PID)"

# Start stream-processor
echo "Starting stream-processor..."
cd "$PROJECT_ROOT/stream-processor"
nohup mvn spring-boot:run > ../stream-processor.log 2>&1 &
STREAM_PROCESSOR_PID=$!
echo "Stream processor started (PID: $STREAM_PROCESSOR_PID)"

# Start query-api
echo "Starting query-api..."
cd "$PROJECT_ROOT/query-api"
nohup mvn spring-boot:run > ../query-api.log 2>&1 &
QUERY_API_PID=$!
echo "Query API started (PID: $QUERY_API_PID)"

echo ""
echo "Waiting for services to start..."
sleep 30

# Check service health
echo ""
echo "Checking service health..."
for port in 8081 8082 8083; do
    if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
        echo "✅ Service on port $port is healthy"
    else
        echo "❌ Service on port $port is not responding"
    fi
done

echo ""
echo "Services started. Logs are in:"
echo "  - log-producer.log"
echo "  - stream-processor.log"
echo "  - query-api.log"
echo ""
echo "To stop services, run: pkill -f 'spring-boot:run'"
