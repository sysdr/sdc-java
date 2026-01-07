#!/bin/bash

echo "🚀 Starting Kafka Log Producers System..."

# Start infrastructure
echo "Starting Docker Compose services..."
docker compose up -d

echo "Waiting for services to be healthy..."
sleep 30

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
until docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  echo "Kafka is unavailable - sleeping"
  sleep 2
done

echo "✅ Kafka is ready!"

# Create Kafka topics
echo "Creating Kafka topics..."
docker compose exec -T kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic application-logs

docker compose exec -T kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic infrastructure-metrics

docker compose exec -T kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic transaction-logs

echo "✅ Kafka topics created!"

echo ""
echo "================================================"
echo "🎉 System is ready!"
echo "================================================"
echo ""
echo "Services:"
echo "  - Log Gateway: http://localhost:8080"
echo "  - Application Log Shipper: http://localhost:8081"
echo "  - Infrastructure Log Shipper: http://localhost:8082"
echo "  - Transaction Log Shipper: http://localhost:8083"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "To test the system:"
echo "  ./integration-tests/test-producers.sh"
echo ""
echo "To run load test:"
echo "  ./load-test.sh"
echo ""
echo "To view logs:"
echo "  docker compose logs -f [service-name]"
echo ""
echo "To stop the system:"
echo "  docker compose down"
echo ""
