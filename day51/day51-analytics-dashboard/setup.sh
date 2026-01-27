#!/bin/bash

echo "🚀 Setting up Real-Time Analytics Dashboard System..."

# Start infrastructure
echo "Starting infrastructure services..."
docker-compose up -d zookeeper kafka redis postgres

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
sleep 30

# Build and start application services
echo "Building application services..."
docker-compose build

echo "Starting application services..."
docker-compose up -d

echo "Waiting for services to initialize..."
sleep 45

echo "✅ Setup complete!"
echo ""
echo "📊 Dashboard: http://localhost:8080"
echo "📈 Prometheus: http://localhost:9090"
echo "📉 Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Run './load-test.sh' to generate traffic"
