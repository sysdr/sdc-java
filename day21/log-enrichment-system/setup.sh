#!/bin/bash

echo "🚀 Setting up Log Enrichment System..."

# Start infrastructure
echo "Starting Docker containers..."
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 30

# Check Kafka topics
echo "Creating Kafka topics..."
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --create --if-not-exists --topic raw-logs --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --create --if-not-exists --topic enriched-logs-complete --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --create --if-not-exists --topic enriched-logs-partial --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Set Redis system environment
echo "Configuring Redis..."
docker exec -it $(docker ps -qf "name=redis") redis-cli SET system:environment production

echo "✅ Infrastructure setup complete!"
echo ""
echo "Next steps:"
echo "1. Build services: mvn clean package"
echo "2. Start services:"
echo "   - java -jar log-producer/target/log-producer-1.0.0.jar"
echo "   - java -jar metadata-service/target/metadata-service-1.0.0.jar"
echo "   - java -jar enrichment-service/target/enrichment-service-1.0.0.jar"
echo "3. Run load tests: ./load-test.sh"
echo ""
echo "Access points:"
echo "- Log Producer API: http://localhost:8080"
echo "- Enrichment Service: http://localhost:8081"
echo "- Metadata Service: http://localhost:8082"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
