#!/bin/bash

set -e

echo "Starting Backpressure Log Processing System..."

# Start infrastructure
docker-compose up -d

echo "Waiting for services to be ready..."
sleep 15

# Create Kafka topic
docker exec -it $(docker ps -q -f name=kafka) kafka-topics \
  --create --topic log-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists || true

echo "Infrastructure ready!"
echo ""
echo "Access points:"
echo "  API Gateway:   http://localhost:8080"
echo "  Log Consumer:  http://localhost:8081"
echo "  Prometheus:    http://localhost:9090"
echo "  Grafana:       http://localhost:3000 (admin/admin)"
echo ""
echo "To build and run services:"
echo "  cd api-gateway && mvn spring-boot:run"
echo "  cd log-consumer && mvn spring-boot:run"
