#!/bin/bash

echo "Building Log Normalization System..."

# Build all modules
mvn clean package -DskipTests

# Start infrastructure
docker compose up -d zookeeper kafka redis prometheus grafana

echo "Waiting for Kafka to be ready..."
sleep 15

# Create topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic raw-logs --partitions 3 --replication-factor 1
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic normalized-logs-json --partitions 3 --replication-factor 1
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic normalized-logs-avro --partitions 3 --replication-factor 1
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic normalized-logs-protobuf --partitions 3 --replication-factor 1
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic normalization-dlq --partitions 1 --replication-factor 1

echo "Starting services..."
docker compose up -d normalizer-service log-producer api-gateway

echo ""
echo "System ready!"
echo "  API Gateway:    http://localhost:8080"
echo "  Normalizer:     http://localhost:8081"
echo "  Producer:       http://localhost:8082"
echo "  Prometheus:     http://localhost:9090"
echo "  Grafana:        http://localhost:3000 (admin/admin)"
