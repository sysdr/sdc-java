#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting all services..."

# Kill any existing services
echo "Checking for existing services..."
pkill -f "log-producer" || true
pkill -f "log-consumer" || true
pkill -f "api-gateway" || true
sleep 2

# Start Producer
echo "Starting log-producer on port 8081..."
cd "$SCRIPT_DIR/log-producer"
nohup mvn spring-boot:run > /tmp/producer.log 2>&1 &
echo $! > /tmp/producer.pid
echo "Producer PID: $(cat /tmp/producer.pid)"

# Start Consumer Instance 1
echo "Starting log-consumer instance 1 on port 8082..."
cd "$SCRIPT_DIR/log-consumer"
SERVER_PORT=8082 nohup mvn spring-boot:run > /tmp/consumer1.log 2>&1 &
echo $! > /tmp/consumer1.pid
echo "Consumer 1 PID: $(cat /tmp/consumer1.pid)"

# Start Gateway
echo "Starting api-gateway on port 8080..."
cd "$SCRIPT_DIR/api-gateway"
nohup mvn spring-boot:run > /tmp/gateway.log 2>&1 &
echo $! > /tmp/gateway.pid
echo "Gateway PID: $(cat /tmp/gateway.pid)"

echo ""
echo "Waiting for services to start (30 seconds)..."
sleep 30

echo ""
echo "Checking service status..."
echo "Producer (8081):"
curl -s http://localhost:8081/api/logs/health || echo "  Not ready yet"

echo ""
echo "Consumer (8082):"
curl -s http://localhost:8082/api/consumer/health || echo "  Not ready yet"

echo ""
echo "Gateway (8080):"
curl -s http://localhost:8080/api/health || echo "  Not ready yet"

echo ""
echo "Service PIDs:"
echo "  Producer: $(cat /tmp/producer.pid 2>/dev/null || echo 'N/A')"
echo "  Consumer 1: $(cat /tmp/consumer1.pid 2>/dev/null || echo 'N/A')"
echo "  Gateway: $(cat /tmp/gateway.pid 2>/dev/null || echo 'N/A')"

echo ""
echo "Log files:"
echo "  Producer: /tmp/producer.log"
echo "  Consumer 1: /tmp/consumer1.log"
echo "  Gateway: /tmp/gateway.log"

echo ""
echo "To stop services: ./stop-all-services.sh"

