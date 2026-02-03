#!/bin/bash

set -e

PROJECT_DIR="/home/systemdr03/git/sdc-java/day59/day59-active-passive-failover"
cd "$PROJECT_DIR"

echo "Starting services..."

# Check if JARs exist
if [ ! -f "$PROJECT_DIR/api-gateway/target/api-gateway-1.0.0.jar" ]; then
    echo "ERROR: API Gateway JAR not found!"
    exit 1
fi

if [ ! -f "$PROJECT_DIR/log-consumer/target/log-consumer-1.0.0.jar" ]; then
    echo "ERROR: Log Consumer JAR not found!"
    exit 1
fi

# Kill any existing processes
pkill -f "api-gateway-1.0.0.jar" || true
pkill -f "log-consumer-1.0.0.jar" || true
sleep 2

# Start API Gateway
echo "Starting API Gateway on port 8080..."
nohup java -jar "$PROJECT_DIR/api-gateway/target/api-gateway-1.0.0.jar" > /tmp/api-gateway.log 2>&1 &
API_GATEWAY_PID=$!
echo "API Gateway started with PID: $API_GATEWAY_PID"

# Wait for API Gateway to start
sleep 10

# Start first Consumer
echo "Starting Log Consumer 1 on port 8081..."
nohup java -jar "$PROJECT_DIR/log-consumer/target/log-consumer-1.0.0.jar" > /tmp/log-consumer-1.log 2>&1 &
CONSUMER1_PID=$!
echo "Log Consumer 1 started with PID: $CONSUMER1_PID"

# Wait a bit
sleep 5

# Start second Consumer
echo "Starting Log Consumer 2 on port 8082..."
SERVER_PORT=8082 nohup java -jar "$PROJECT_DIR/log-consumer/target/log-consumer-1.0.0.jar" > /tmp/log-consumer-2.log 2>&1 &
CONSUMER2_PID=$!
echo "Log Consumer 2 started with PID: $CONSUMER2_PID"

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 15

# Check if services are running
if ps -p $API_GATEWAY_PID > /dev/null; then
    echo "✅ API Gateway is running (PID: $API_GATEWAY_PID)"
else
    echo "❌ API Gateway failed to start. Check /tmp/api-gateway.log"
    cat /tmp/api-gateway.log
    exit 1
fi

if ps -p $CONSUMER1_PID > /dev/null; then
    echo "✅ Log Consumer 1 is running (PID: $CONSUMER1_PID)"
else
    echo "❌ Log Consumer 1 failed to start. Check /tmp/log-consumer-1.log"
    cat /tmp/log-consumer-1.log
    exit 1
fi

if ps -p $CONSUMER2_PID > /dev/null; then
    echo "✅ Log Consumer 2 is running (PID: $CONSUMER2_PID)"
else
    echo "❌ Log Consumer 2 failed to start. Check /tmp/log-consumer-2.log"
    cat /tmp/log-consumer-2.log
    exit 1
fi

echo ""
echo "Services started successfully!"
echo "API Gateway: http://localhost:8080"
echo "Consumer 1: http://localhost:8081"
echo "Consumer 2: http://localhost:8082"
