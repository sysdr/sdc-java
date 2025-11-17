#!/bin/bash

echo "Starting Protocol Buffers Log Processing System..."

# Check for JDK
if ! command -v javac &> /dev/null; then
    echo "ERROR: JDK (javac) is not installed. Only JRE is available."
    echo "Please install the JDK with:"
    echo "  sudo apt-get update && sudo apt-get install -y openjdk-21-jdk"
    echo ""
    echo "After installation, set JAVA_HOME:"
    echo "  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
    exit 1
fi

# Verify javac version
JAVAC_VERSION=$(javac -version 2>&1)
echo "Found JDK: $JAVAC_VERSION"

# Start infrastructure
echo "Starting Docker containers..."
docker-compose up -d

# Wait for services
echo "Waiting for Kafka to be ready..."
sleep 30

# Create Kafka topic
echo "Creating Kafka topic..."
docker exec $(docker ps -qf "name=kafka") kafka-topics --create \
  --topic log-events-protobuf \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

echo "Infrastructure is ready!"
echo ""
echo "Build and start services:"
echo "  cd protobuf-log-system"
echo "  mvn clean install"
echo "  java -jar log-producer/target/log-producer-1.0.0.jar &"
echo "  java -jar log-consumer/target/log-consumer-1.0.0.jar &"
echo "  java -jar api-gateway/target/api-gateway-1.0.0.jar &"
echo ""
echo "Services:"
echo "  Producer API: http://localhost:8081"
echo "  Consumer: http://localhost:8082"
echo "  Gateway: http://localhost:8080"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin/admin)"
