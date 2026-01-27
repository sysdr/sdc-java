#!/bin/bash

echo "=== Setting up Anomaly Detection System ==="

# Check Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker first."
    exit 1
fi

# Check Docker Compose is installed and determine command
DOCKER_COMPOSE_CMD=""
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
else
    echo "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Stop existing containers
echo "Stopping existing containers..."
$DOCKER_COMPOSE_CMD down -v

# Build and start services
echo "Building and starting services..."
$DOCKER_COMPOSE_CMD up -d --build

echo "Waiting for services to be ready..."
sleep 60

# Create Kafka topics
echo "Creating Kafka topics..."
$DOCKER_COMPOSE_CMD exec -T kafka kafka-topics --create --topic log-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
$DOCKER_COMPOSE_CMD exec -T kafka kafka-topics --create --topic detected-anomalies --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists

echo "=== Setup Complete ==="
echo ""
echo "Services running:"
echo "  - API Gateway: http://localhost:8080"
echo "  - Log Producer: http://localhost:8081"
echo "  - Anomaly Detector: http://localhost:8082"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Run integration tests: ./integration-tests/test-anomaly-detection.sh"
echo "Run load tests: ./load-test.sh"
