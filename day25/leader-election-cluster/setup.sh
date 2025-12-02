#!/bin/bash

echo "🚀 Setting up Leader Election Cluster..."

# Build projects
echo "📦 Building Maven projects..."
mvn clean package -DskipTests

# Build Docker images
echo "🐳 Building Docker images..."
docker build -t storage-node:latest ./storage-node
docker build -t api-gateway:latest ./api-gateway

# Start infrastructure
echo "🎯 Starting infrastructure..."
docker-compose up -d postgres redis

# Wait for databases
echo "⏳ Waiting for databases..."
sleep 10

# Start storage nodes
echo "📊 Starting storage nodes..."
docker-compose up -d storage-node-1 storage-node-2 storage-node-3

# Wait for nodes to elect leader
echo "🗳️ Waiting for leader election..."
sleep 15

# Start gateway
echo "🚪 Starting API gateway..."
docker-compose up -d api-gateway

# Start monitoring
echo "📈 Starting monitoring..."
docker-compose up -d prometheus grafana

sleep 5

echo ""
echo "✅ Leader Election Cluster is ready!"
echo ""
echo "Services:"
echo "  API Gateway: http://localhost:8080"
echo "  Storage Node 1: http://localhost:8081"
echo "  Storage Node 2: http://localhost:8082"
echo "  Storage Node 3: http://localhost:8083"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin/admin)"
echo "  Dashboard: http://localhost:8000 (run: python3 dashboard/server.py)"
echo ""
echo "Check leader status:"
echo "  curl http://localhost:8080/api/status"
echo ""
echo "Test write:"
echo '  curl -X POST http://localhost:8080/api/write -H "Content-Type: application/json" -d '"'"'{"data":"test log entry"}'"'"
echo ""
echo "Start Dashboard:"
echo "  python3 dashboard/server.py"
