#!/bin/bash

echo "🚀 Setting up Compressed Log Processing System..."

# Start infrastructure
echo "Starting Docker containers..."
docker compose up -d

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 30

# Check Kafka
echo "Checking Kafka..."
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --bootstrap-server localhost:9092 --list || true

echo "✅ Infrastructure ready!"
echo ""
echo "📊 Access points:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - API Gateway: http://localhost:8080"
echo "  - Log Producer: http://localhost:8081"
echo "  - Log Consumer: http://localhost:8082"
echo ""
echo "Next steps:"
echo "  1. Build services: mvn clean package"
echo "  2. Run log-producer: cd log-producer && mvn spring-boot:run"
echo "  3. Run log-consumer: cd log-consumer && mvn spring-boot:run"
echo "  4. Run api-gateway: cd api-gateway && mvn spring-boot:run"
echo "  5. Run load test: ./load-test.sh"
