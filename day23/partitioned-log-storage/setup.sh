#!/bin/bash

echo "🚀 Setting up Partitioned Log Storage System..."

# Start infrastructure
echo "Starting Docker infrastructure..."
docker compose up -d

# Wait for services to be ready
echo "Waiting for PostgreSQL..."
until docker compose exec -T postgres pg_isready -U loguser; do
  sleep 2
done

echo "Waiting for Kafka..."
sleep 10

# Build all services
echo "Building services..."
mvn clean package -DskipTests

echo "✅ Setup complete!"
echo ""
echo "Start services:"
echo "  java -jar partition-manager/target/partition-manager-1.0.0.jar"
echo "  java -jar log-consumer/target/log-consumer-1.0.0.jar"
echo "  java -jar log-producer/target/log-producer-1.0.0.jar"
echo "  java -jar query-service/target/query-service-1.0.0.jar"
echo ""
echo "Access points:"
echo "  Grafana: http://localhost:3000 (admin/admin)"
echo "  Prometheus: http://localhost:9090"
echo "  Producer API: http://localhost:8081"
echo "  Query API: http://localhost:8084"
echo ""
echo "Setup Dashboard:"
echo "  cd dashboard-server"
echo "  npm install"
echo "  npm start"
echo "  Dashboard: http://localhost:3001"
