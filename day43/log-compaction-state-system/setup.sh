#!/bin/bash

set -e

echo "🚀 Starting Log Compaction State Management System..."

# Start infrastructure
echo "📦 Starting Docker containers..."
docker-compose up -d zookeeper kafka postgres redis prometheus grafana

echo "⏳ Waiting for Kafka to be ready..."
sleep 30

# Create compacted topic
echo "📝 Creating compacted entity state topic..."
docker-compose exec -T kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic entity-state-compacted \
  --partitions 24 \
  --replication-factor 1 \
  --config cleanup.policy=compact \
  --config min.cleanable.dirty.ratio=0.5 \
  --config segment.ms=86400000 \
  --config delete.retention.ms=86400000 \
  --config min.compaction.lag.ms=0 \
  --if-not-exists

echo "✅ Compacted topic created successfully"

# Build and start services
echo "🔨 Building services..."
docker-compose build state-producer state-consumer state-query-api

echo "🚀 Starting application services..."
docker-compose up -d state-producer state-consumer state-query-api

echo ""
echo "✅ System is ready!"
echo ""
echo "📊 Service URLs:"
echo "  - State Producer API: http://localhost:8081"
echo "  - State Consumer: http://localhost:8082"
echo "  - State Query API: http://localhost:8083"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "🔍 Health checks:"
echo "  curl http://localhost:8081/actuator/health"
echo "  curl http://localhost:8082/actuator/health"
echo "  curl http://localhost:8083/actuator/health"
