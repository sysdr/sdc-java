#!/bin/bash

echo "🚀 Setting up Distributed Log Query System..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Stop any existing containers
echo "🛑 Stopping existing containers..."
docker-compose down -v

# Build and start services
echo "🔨 Building services..."
docker-compose build

echo "🚀 Starting services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
echo "   This may take 2-3 minutes..."

# Wait for Kafka
echo "   - Waiting for Kafka..."
sleep 30

# Wait for partition nodes
echo "   - Waiting for partition nodes..."
for i in {1..30}; do
    if curl -s http://localhost:8081/api/partition/health > /dev/null 2>&1 && \
       curl -s http://localhost:8082/api/partition/health > /dev/null 2>&1 && \
       curl -s http://localhost:8083/api/partition/health > /dev/null 2>&1; then
        echo "   ✅ Partition nodes are healthy"
        break
    fi
    sleep 2
done

# Wait for coordinator
echo "   - Waiting for query coordinator..."
for i in {1..20}; do
    if curl -s http://localhost:8080/api/query/health > /dev/null 2>&1; then
        echo "   ✅ Query coordinator is healthy"
        break
    fi
    sleep 2
done

echo ""
echo "✅ System is ready!"
echo ""
echo "📊 Service URLs:"
echo "   - Dashboard:         http://localhost:5000"
echo "   - Query Coordinator: http://localhost:8080"
echo "   - Partition Node 1:  http://localhost:8081"
echo "   - Partition Node 2:  http://localhost:8082"
echo "   - Partition Node 3:  http://localhost:8083"
echo "   - Prometheus:        http://localhost:9090"
echo "   - Grafana:           http://localhost:3000 (admin/admin)"
echo ""
echo "🧪 Run load tests:"
echo "   ./load-test.sh"
echo ""
echo "📋 View logs:"
echo "   docker-compose logs -f query-coordinator"
echo "   docker-compose logs -f partition-node-1"
echo ""
echo "🔍 Example query:"
echo "   curl -X POST http://localhost:8080/api/query/logs \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"startTime\": \"2024-01-01T00:00:00Z\", \"endTime\": \"2024-12-31T23:59:59Z\", \"limit\": 100}'"
