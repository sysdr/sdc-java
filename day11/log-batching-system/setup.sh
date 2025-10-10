#!/bin/bash

set -e

echo "Setting up Log Batching System..."

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running"
    exit 1
fi

# Start infrastructure
echo "Starting infrastructure services..."
docker-compose up -d zookeeper kafka postgres redis prometheus grafana

# Wait for infrastructure to be ready
echo "Waiting for infrastructure to be ready..."
sleep 30

# Check Kafka is ready
echo "Checking Kafka..."
until docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
    echo "Waiting for Kafka..."
    sleep 5
done

# Create Kafka topic
echo "Creating Kafka topic..."
docker-compose exec -T kafka kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --topic log-events \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists

# Start application services
echo "Building and starting application services..."
docker-compose up -d --build log-shipper log-consumer api-gateway log-producer

echo ""
echo "System is starting up..."
echo "Services will be available in ~60 seconds"
echo ""
echo "Endpoints:"
echo "  - API Gateway:  http://localhost:8080"
echo "  - Log Producer: http://localhost:8081"
echo "  - Log Shipper:  http://localhost:8082"
echo "  - Log Consumer: http://localhost:8083"
echo "  - Prometheus:   http://localhost:9090"
echo "  - Grafana:      http://localhost:3000 (admin/admin)"
echo ""
echo "Run './integration-tests/test-system.sh' to verify the system"
echo "Run './load-test.sh' to generate load"
