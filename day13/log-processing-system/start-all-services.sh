#!/bin/bash

echo "🚀 Starting all Spring Boot services..."

# Kill any existing processes
pkill -f "log-producer" 2>/dev/null || true
pkill -f "log-consumer" 2>/dev/null || true
pkill -f "api-gateway" 2>/dev/null || true

sleep 2

# Create logs directory
mkdir -p logs

# Start API Gateway
echo "Starting API Gateway..."
cd api-gateway && mvn spring-boot:run > ../logs/api-gateway.log 2>&1 &
API_GATEWAY_PID=$!
echo "API Gateway PID: $API_GATEWAY_PID"

# Start Log Producer
echo "Starting Log Producer..."
cd ../log-producer && mvn spring-boot:run > ../logs/log-producer.log 2>&1 &
LOG_PRODUCER_PID=$!
echo "Log Producer PID: $LOG_PRODUCER_PID"

# Start Log Consumer
echo "Starting Log Consumer..."
cd ../log-consumer && DB_HOST=127.0.0.1 DB_PASSWORD=postgres mvn spring-boot:run > ../logs/log-consumer.log 2>&1 &
LOG_CONSUMER_PID=$!
echo "Log Consumer PID: $LOG_CONSUMER_PID"

echo "Waiting for services to start..."
sleep 15

echo "Checking service status:"
echo "API Gateway (port 8080):"
curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' || echo "Not responding"

echo "Log Producer (port 8081):"
curl -s http://localhost:8081/actuator/health | grep -o '"status":"[^"]*"' || echo "Not responding"

echo "Log Consumer (port 8082):"
curl -s http://localhost:8082/actuator/health | grep -o '"status":"[^"]*"' || echo "Not responding"

echo ""
echo "Dashboard: http://localhost:3001"
echo "Logs directory: ./logs/"
echo ""
echo "To stop all services: pkill -f 'spring-boot:run'"
