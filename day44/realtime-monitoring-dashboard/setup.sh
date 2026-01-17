#!/bin/bash

echo "Setting up Real-Time Monitoring Dashboard..."

# Build and start all services
echo "Building services..."
docker-compose build

echo "Starting infrastructure..."
docker-compose up -d zookeeper kafka redis prometheus grafana

echo "Waiting for Kafka to be ready..."
sleep 30

echo "Creating Kafka topics..."
docker-compose exec -T kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic log-events \
  --partitions 12 \
  --replication-factor 1 \
  --if-not-exists

echo "Starting application services..."
docker-compose up -d log-producer stream-processor dashboard-api

echo "Waiting for services to be healthy..."
sleep 20

echo ""
echo "================================================"
echo "Real-Time Monitoring Dashboard is ready!"
echo "================================================"
echo ""
echo "Services:"
echo "  - Dashboard UI: http://localhost:8083"
echo "  - Log Producer: http://localhost:8081"
echo "  - Stream Processor: http://localhost:8082"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f stream-processor"
echo ""
echo "To stop all services:"
echo "  docker-compose down"
echo ""
