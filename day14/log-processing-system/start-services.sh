#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if required directories exist
if [ ! -d "log-producer" ]; then
    echo "Error: log-producer directory not found. Please run the setup script first."
    exit 1
fi

if [ ! -d "log-consumer" ]; then
    echo "Error: log-consumer directory not found. Please run the setup script first."
    exit 1
fi

if [ ! -d "api-gateway" ]; then
    echo "Error: api-gateway directory not found. Please run the setup script first."
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p logs

echo "Starting all services from $SCRIPT_DIR..."

# Start producer
cd "$SCRIPT_DIR/log-producer"
mvn spring-boot:run > "$SCRIPT_DIR/logs/producer.log" 2>&1 &
PRODUCER_PID=$!
echo "Started log-producer (PID: $PRODUCER_PID)"

# Wait for producer to start
sleep 10

# Start consumer
cd "$SCRIPT_DIR/log-consumer"
mvn spring-boot:run > "$SCRIPT_DIR/logs/consumer.log" 2>&1 &
CONSUMER_PID=$!
echo "Started log-consumer (PID: $CONSUMER_PID)"

# Wait for consumer to start
sleep 10

# Start gateway
cd "$SCRIPT_DIR/api-gateway"
mvn spring-boot:run > "$SCRIPT_DIR/logs/gateway.log" 2>&1 &
GATEWAY_PID=$!
echo "Started api-gateway (PID: $GATEWAY_PID)"

cd "$SCRIPT_DIR"

echo ""
echo "All services started!"
echo "  - Producer: http://localhost:8081"
echo "  - Consumer: http://localhost:8082"
echo "  - Gateway: http://localhost:8080"
echo ""
echo "PIDs: Producer=$PRODUCER_PID Consumer=$CONSUMER_PID Gateway=$GATEWAY_PID"
echo "Logs are in $SCRIPT_DIR/logs/"
echo ""
echo "To stop services: kill $PRODUCER_PID $CONSUMER_PID $GATEWAY_PID"
