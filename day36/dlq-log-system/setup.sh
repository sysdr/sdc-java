#!/bin/bash

echo "Starting Dead Letter Queue System..."

# Start infrastructure
docker-compose up -d

# Wait for services to be ready
echo "Waiting for Kafka to be ready..."
sleep 20

# Create Kafka topics
docker exec dlq-log-system-kafka-1 kafka-topics --create --topic log-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
docker exec dlq-log-system-kafka-1 kafka-topics --create --topic log-events-retry --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
docker exec dlq-log-system-kafka-1 kafka-topics --create --topic log-events-dlq --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true

echo "Infrastructure ready!"
echo "Kafka: localhost:9092"
echo "PostgreSQL: localhost:5432"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Build and run services:"
echo "  cd log-producer && mvn spring-boot:run"
echo "  cd log-consumer && mvn spring-boot:run"
echo "  cd api-gateway && mvn spring-boot:run"
