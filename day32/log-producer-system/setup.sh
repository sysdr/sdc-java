#!/bin/bash

set -e

echo "======================================"
echo "Setting up Log Producer System"
echo "======================================"
echo ""

# Start infrastructure
echo "Starting Docker infrastructure..."
docker-compose up -d

echo "Waiting for services to be ready..."
sleep 30

# Check Kafka is ready
echo "Checking Kafka..."
docker exec kafka kafka-topics --bootstrap-server localhost:29092 --list || true

# Create topic if it doesn't exist
echo "Creating Kafka topic 'logs'..."
docker exec kafka kafka-topics --bootstrap-server localhost:29092 \
  --create --if-not-exists --topic logs --partitions 3 --replication-factor 1

echo ""
echo "✅ Infrastructure setup complete!"
echo ""
echo "Services running:"
echo "  - Kafka: localhost:9092"
echo "  - Redis: localhost:6379"
echo "  - PostgreSQL: localhost:5432"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Next steps:"
echo "  1. Build services: mvn clean package"
echo "  2. Start producer: cd log-producer && mvn spring-boot:run"
echo "  3. Start consumer: cd log-consumer && mvn spring-boot:run"
echo "  4. Start gateway: cd api-gateway && mvn spring-boot:run"
echo "  5. Run load test: ./load-test.sh"
echo ""
