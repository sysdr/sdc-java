#!/bin/bash

echo "Starting Kafka cluster and supporting infrastructure..."
docker compose up -d

echo "Waiting for services to be ready..."
sleep 30

echo "Checking service health..."
docker ps

echo ""
echo "Infrastructure ready!"
echo "Kafka brokers: localhost:9092, localhost:9093, localhost:9094"
echo "PostgreSQL: localhost:5432"
echo "Redis: localhost:6379"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
