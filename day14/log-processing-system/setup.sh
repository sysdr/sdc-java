#!/bin/bash

set -e

echo "Starting Log Processing System infrastructure..."

# Start Docker Compose
docker compose up -d

echo "Waiting for services to be healthy..."
sleep 30

# Check service health
echo "Checking Kafka..."
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list || echo "Kafka not ready yet"

echo "Checking PostgreSQL..."
docker compose exec postgres pg_isready -U postgres || echo "PostgreSQL not ready yet"

echo "Checking Redis..."
docker compose exec redis redis-cli ping || echo "Redis not ready yet"

echo ""
echo "Infrastructure is starting up. Services will be available at:"
echo "  - Kafka: localhost:9092"
echo "  - PostgreSQL: localhost:5432"
echo "  - Redis: localhost:6379"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "To start the Spring Boot applications:"
echo "  1. cd log-producer && mvn spring-boot:run"
echo "  2. cd log-consumer && mvn spring-boot:run"
echo "  3. cd api-gateway && mvn spring-boot:run"
echo ""
echo "Or build and run all with Maven:"
echo "  mvn clean install && ./start-services.sh"
