#!/bin/bash

set -e

echo "🚀 Starting TLS-secured distributed log processing system..."

# Generate certificates
echo "Step 1: Generating TLS certificates..."
cd certs
./generate-certs.sh
cd ..

# Create monitoring directories
mkdir -p monitoring/dashboards

# Start infrastructure
echo "Step 2: Starting infrastructure services..."
docker-compose up -d zookeeper kafka postgres redis

echo "Waiting for Kafka to be ready (30 seconds)..."
sleep 30

# Create Kafka topic
echo "Step 3: Creating Kafka topics..."
docker-compose exec -T kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic log-events \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists || true

# Start monitoring
echo "Step 4: Starting monitoring dashboard..."
docker-compose up -d dashboard

# Build services
echo "Step 5: Building Spring Boot services..."
mvn clean package -DskipTests

# Copy certificates to services
for service in log-producer log-consumer api-gateway; do
  mkdir -p ${service}/src/main/resources/certs
  cp certs/keystore.jks ${service}/src/main/resources/certs/
  cp certs/truststore.jks ${service}/src/main/resources/certs/
done

echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Start services: cd log-producer && mvn spring-boot:run"
echo "2. View logs: docker-compose logs -f"
echo "3. Access Dashboard: http://localhost:3001"
echo "4. Run load tests: ./load-test.sh"
