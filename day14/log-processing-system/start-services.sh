#!/bin/bash

echo "Starting all services..."

# Start producer
cd log-producer
mvn spring-boot:run > ../logs/producer.log 2>&1 &
PRODUCER_PID=$!
echo "Started log-producer (PID: $PRODUCER_PID)"

# Wait for producer to start
sleep 10

# Start consumer
cd ../log-consumer
mvn spring-boot:run > ../logs/consumer.log 2>&1 &
CONSUMER_PID=$!
echo "Started log-consumer (PID: $CONSUMER_PID)"

# Wait for consumer to start
sleep 10

# Start gateway
cd ../api-gateway
mvn spring-boot:run > ../logs/gateway.log 2>&1 &
GATEWAY_PID=$!
echo "Started api-gateway (PID: $GATEWAY_PID)"

cd ..

echo ""
echo "All services started!"
echo "  - Producer: http://localhost:8081"
echo "  - Consumer: http://localhost:8082"
echo "  - Gateway: http://localhost:8080"
echo ""
echo "PIDs: Producer=$PRODUCER_PID Consumer=$CONSUMER_PID Gateway=$GATEWAY_PID"
echo "Logs are in ./logs/"
echo ""
echo "To stop services: kill $PRODUCER_PID $CONSUMER_PID $GATEWAY_PID"
