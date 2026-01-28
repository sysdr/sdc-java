#!/bin/bash

set -e

echo "🚀 Setting up Day 54: Query Language System"

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is required but not installed"
    exit 1
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is required but not installed"
    exit 1
fi

echo "📦 Starting infrastructure..."
docker-compose up -d zookeeper kafka redis postgres

echo "⏳ Waiting for infrastructure (30s)..."
sleep 30

echo "🏗️ Starting application services..."
docker-compose up -d query-coordinator query-executor-1 query-executor-2 query-executor-3 log-producer

echo "⏳ Waiting for services to be ready (30s)..."
sleep 30

echo "📊 Starting monitoring..."
docker-compose up -d prometheus grafana

echo "✅ System is ready!"
echo ""
echo "🌐 Access Points:"
echo "  Query API:    http://localhost:8080/api/query"
echo "  Executor 1:   http://localhost:8081/api/health"
echo "  Executor 2:   http://localhost:8082/api/health"
echo "  Executor 3:   http://localhost:8083/api/health"
echo "  Prometheus:   http://localhost:9090"
echo "  Grafana:      http://localhost:3000 (admin/admin)"
echo ""
echo "📝 Try a query:"
echo "  curl -X POST http://localhost:8080/api/query \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"query\": \"SELECT * WHERE level = '\\''ERROR'\\'' LIMIT 10\"}'"
echo ""
echo "🧪 Run tests:"
echo "  cd integration-tests && ./test-queries.sh"
echo "  ./load-test.sh"
