#!/bin/bash

echo "=== Starting Chaos Testing Infrastructure ==="

# Start Docker infrastructure
docker compose up -d

echo "Waiting for services to be healthy..."
sleep 20

# Check service health
echo "Checking Kafka..."
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null || echo "Kafka not ready yet"

echo "Checking PostgreSQL..."
docker compose exec postgres pg_isready -U loguser

echo "Checking Redis..."
docker compose exec redis redis-cli ping

echo ""
echo "=== Infrastructure Ready ==="
echo "Kafka: localhost:9092"
echo "PostgreSQL: localhost:5432 (user: loguser, db: logdb)"
echo "Redis: localhost:6379"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Next steps:"
echo "1. Build services: mvn clean install"
echo "2. Start producer: cd log-producer && mvn spring-boot:run"
echo "3. Start consumer: cd log-consumer && mvn spring-boot:run"
echo "4. Start gateway: cd api-gateway && mvn spring-boot:run"
echo "5. Run chaos tests: cd chaos-engine && mvn test"
