#!/bin/bash

echo "🚀 Setting up Alert Generation System"
echo "====================================="

# Start infrastructure
echo "Starting Docker containers..."
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to start..."
echo "This may take 2-3 minutes for first-time setup (building images)..."
sleep 60

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
for i in {1..30}; do
  if docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo "Kafka is ready!"
    break
  fi
  echo "Waiting for Kafka... ($i/30)"
  sleep 2
done

# Check Kafka topics
echo "Creating Kafka topics..."
docker-compose exec -T kafka kafka-topics --create --if-not-exists \
  --topic log-events --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1 || echo "Topic log-events may already exist"

docker-compose exec -T kafka kafka-topics --create --if-not-exists \
  --topic alerts --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1 || echo "Topic alerts may already exist"

docker-compose exec -T kafka kafka-topics --create --if-not-exists \
  --topic notifications --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1 || echo "Topic notifications may already exist"

# Wait additional time for application services to start
echo "Waiting for application services to start..."
sleep 30

# Verify services
echo -e "\nVerifying services..."
services=(
  "http://localhost:8080/health"
  "http://localhost:8081/health"
  "http://localhost:8082/health"
  "http://localhost:8083/health"
)

for service in "${services[@]}"; do
  if curl -s "$service" | grep -q "UP"; then
    echo "✅ $service is UP"
  else
    echo "❌ $service is DOWN"
  fi
done

echo -e "\n✅ Setup complete!"
echo -e "\nAccess points:"
echo "  API Gateway: http://localhost:8080"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin/admin)"
echo -e "\nRun integration tests: ./integration-tests/test-alert-flow.sh"
echo "Run load test: ./load-test.sh"
