#!/bin/bash

cd "$(dirname "$0")"

echo "Starting API Gateway..."
cd api-gateway
nohup java -jar target/api-gateway-1.0.0.jar > ../logs/api-gateway.log 2>&1 &
API_GATEWAY_PID=$!
echo "API Gateway started with PID: $API_GATEWAY_PID"
echo $API_GATEWAY_PID > ../logs/api-gateway.pid

cd ../log-producer
echo "Starting Log Producer..."
nohup java -jar target/log-producer-1.0.0.jar > ../logs/log-producer.log 2>&1 &
LOG_PRODUCER_PID=$!
echo "Log Producer started with PID: $LOG_PRODUCER_PID"
echo $LOG_PRODUCER_PID > ../logs/log-producer.pid

echo ""
echo "Services started!"
echo "API Gateway PID: $API_GATEWAY_PID"
echo "Log Producer PID: $LOG_PRODUCER_PID"
echo ""
echo "Waiting for services to be ready..."
sleep 15

# Check if services are running
if ps -p $API_GATEWAY_PID > /dev/null; then
    echo "✅ API Gateway is running"
else
    echo "❌ API Gateway failed to start. Check logs/api-gateway.log"
fi

if ps -p $LOG_PRODUCER_PID > /dev/null; then
    echo "✅ Log Producer is running"
else
    echo "❌ Log Producer failed to start. Check logs/log-producer.log"
fi
