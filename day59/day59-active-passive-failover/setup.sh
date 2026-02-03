#!/bin/bash

set -e

echo "🚀 Setting up Active-Passive Failover System"
echo "============================================="

# Start infrastructure
echo "Starting Docker infrastructure..."
docker-compose up -d

# Wait for services
echo "Waiting for services to be ready..."
sleep 15

# Create Kafka topic
echo "Creating Kafka topic..."
docker-compose exec -T kafka kafka-topics \
    --create \
    --bootstrap-server localhost:9092 \
    --topic log-events \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists || true

echo ""
echo "✅ Infrastructure ready!"
echo ""
echo "Next steps:"
echo "1. Build the applications:"
echo "   mvn clean package"
echo ""
echo "2. Start API Gateway:"
echo "   java -jar api-gateway/target/api-gateway-1.0.0.jar"
echo ""
echo "3. Start first Consumer instance:"
echo "   java -jar log-consumer/target/log-consumer-1.0.0.jar"
echo ""
echo "4. Start second Consumer instance (different port):"
echo "   SERVER_PORT=8082 java -jar log-consumer/target/log-consumer-1.0.0.jar"
echo ""
echo "5. Run integration tests:"
echo "   ./integration-tests/test-failover.sh"
echo ""
echo "6. Run load tests:"
echo "   ./load-test.sh"
echo ""
echo "Services:"
echo "- API Gateway: http://localhost:8080"
echo "- Consumer 1: http://localhost:8081"
echo "- Consumer 2: http://localhost:8082"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo "- ZooKeeper: localhost:2181"
echo "- Kafka: localhost:9092"
echo "- Redis: localhost:6379"
echo "- PostgreSQL: localhost:5432"
