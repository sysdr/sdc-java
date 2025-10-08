#!/bin/bash

set -e

echo "🚀 Setting Up UDP Log Processing System"
echo "========================================"

# Start infrastructure
echo "Starting Docker containers..."
docker compose up -d

echo "Waiting for services to be ready..."
sleep 10

# Wait for Kafka
echo "Waiting for Kafka..."
timeout 60 bash -c 'until docker compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do sleep 2; done' || {
    echo "⚠️  Kafka startup timeout - continuing anyway"
}

# Create Kafka topic
echo "Creating Kafka topics..."
docker compose exec -T kafka kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --topic logs.udp.ingress \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists || echo "Topic may already exist"

echo ""
echo "✅ Infrastructure is ready!"
echo ""
echo "Next steps:"
echo "1. Build the application: mvn clean package"
echo "2. Start log-consumer: cd log-consumer && mvn spring-boot:run"
echo "3. Start log-producer: cd log-producer && mvn spring-boot:run (in new terminal)"
echo "4. Start api-gateway: cd api-gateway && mvn spring-boot:run (in new terminal)"
echo "5. Run integration tests: ./integration-tests/test_system.sh"
echo "6. Run load tests: ./load-test.sh 20 500"
echo ""
echo "📊 Access points:"
echo "- API Gateway: http://localhost:8080"
echo "- Log Producer: http://localhost:8081"
echo "- Log Consumer: http://localhost:8082"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
