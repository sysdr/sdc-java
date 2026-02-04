#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting all services..."

# Start API Gateway
echo "Starting API Gateway..."
cd api-gateway
nohup mvn spring-boot:run > ../logs/api-gateway.log 2>&1 &
API_GATEWAY_PID=$!
echo "API Gateway started with PID: $API_GATEWAY_PID"
cd ..

# Start Log Consumer
echo "Starting Log Consumer..."
cd log-consumer
nohup mvn spring-boot:run > ../logs/log-consumer.log 2>&1 &
LOG_CONSUMER_PID=$!
echo "Log Consumer started with PID: $LOG_CONSUMER_PID"
cd ..

# Create logs directory if it doesn't exist
mkdir -p logs

echo ""
echo "Services starting..."
echo "API Gateway PID: $API_GATEWAY_PID"
echo "Log Consumer PID: $LOG_CONSUMER_PID"
echo ""
echo "Logs:"
echo "  API Gateway: logs/api-gateway.log"
echo "  Log Consumer: logs/log-consumer.log"
echo ""
echo "Waiting 60 seconds for services to start..."
sleep 60

echo ""
echo "Checking service health..."
curl -s http://localhost:8080/api/logs/health 2>&1 | head -5
echo ""
curl -s http://localhost:8081/actuator/health 2>&1 | head -5
