#!/bin/bash

cd /home/systemdr03/git/sdc-java/day45/mapreduce-log-processor

echo "Starting MapReduce Services..."

# Start Coordinator
nohup java -jar mapreduce-coordinator/target/mapreduce-coordinator-1.0.0.jar > /tmp/coordinator.log 2>&1 &
echo "Coordinator starting (PID: $!)"

# Wait for coordinator
sleep 10

# Start API Gateway
nohup java -jar api-gateway/target/api-gateway-1.0.0.jar > /tmp/api-gateway.log 2>&1 &
echo "API Gateway starting (PID: $!)"

# Start Map Worker
nohup java -jar map-worker/target/map-worker-1.0.0.jar > /tmp/map-worker.log 2>&1 &
echo "Map Worker starting (PID: $!)"

# Start Reduce Worker
nohup java -jar reduce-worker/target/reduce-worker-1.0.0.jar > /tmp/reduce-worker.log 2>&1 &
echo "Reduce Worker starting (PID: $!)"

sleep 15

echo ""
echo "Checking service health..."
curl -s http://localhost:8080/actuator/health && echo " - Coordinator"
curl -s http://localhost:8090/actuator/health && echo " - API Gateway"
curl -s http://localhost:8081/actuator/health && echo " - Map Worker"
curl -s http://localhost:8082/actuator/health && echo " - Reduce Worker"

echo ""
echo "Services started. Logs in /tmp/*.log"
