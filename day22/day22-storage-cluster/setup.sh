#!/bin/bash

set -e

echo "Checking for duplicate services..."
# Check if services are already running
if docker-compose ps | grep -q "Up"; then
    echo "Warning: Some services are already running!"
    echo "Stopping existing services..."
    docker-compose down
fi

# Check for processes using the ports
for port in 8080 8081 8082 8083 9090 9091 9092 3000 6379 8888; do
    if lsof -i :$port > /dev/null 2>&1; then
        echo "Warning: Port $port is already in use!"
        echo "Please stop the service using this port or change the configuration."
        exit 1
    fi
done

echo "Building all services..."
mvn clean package -DskipTests

echo "Starting infrastructure..."
docker-compose up -d

echo "Waiting for services to be healthy..."
sleep 30

echo "Checking service health..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1 && \
       curl -s http://localhost:8081/actuator/health > /dev/null 2>&1 && \
       curl -s http://localhost:8082/actuator/health > /dev/null 2>&1 && \
       curl -s http://localhost:8083/actuator/health > /dev/null 2>&1 && \
       curl -s http://localhost:9090/actuator/health > /dev/null 2>&1 && \
       curl -s http://localhost:9091/actuator/health > /dev/null 2>&1; then
        echo "All services are healthy!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "Error: Services did not become healthy in time"
        docker-compose ps
        exit 1
    fi
    echo "Waiting for services... ($i/30)"
    sleep 2
done

echo "Checking service health endpoints..."
curl -s http://localhost:8080/actuator/health | jq . || echo "Coordinator health check failed"
curl -s http://localhost:8081/actuator/health | jq . || echo "Node-1 health check failed"
curl -s http://localhost:8082/actuator/health | jq . || echo "Node-2 health check failed"
curl -s http://localhost:8083/actuator/health | jq . || echo "Node-3 health check failed"
curl -s http://localhost:9090/actuator/health | jq . || echo "Write Gateway health check failed"
curl -s http://localhost:9091/actuator/health | jq . || echo "Read Gateway health check failed"

echo ""
echo "System is ready!"
echo "- Cluster Coordinator: http://localhost:8080"
echo "- Storage Nodes: http://localhost:8081-8083"
echo "- Write Gateway: http://localhost:9090"
echo "- Read Gateway: http://localhost:9091"
echo "- Prometheus: http://localhost:9092"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo "- Dashboard: http://localhost:8888"
