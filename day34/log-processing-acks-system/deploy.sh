#!/bin/bash

echo "=========================================="
echo "Deploying Log Processing System"
echo "=========================================="

echo "Building Docker images..."
docker-compose build

echo "Starting services..."
docker-compose up -d --scale log-consumer=2

echo "Waiting for services to be healthy..."
sleep 30

echo "Checking service health..."
services=("api-gateway:8080" "log-producer:8081")

for service in "${services[@]}"; do
  IFS=':' read -r name port <<< "$service"
  if curl -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
    echo "✓ $name is healthy"
  else
    echo "✗ $name is not responding"
  fi
done

# Check log-consumer instances (no external port, check via docker)
consumer_count=$(docker ps --filter "name=log-consumer" --format "{{.Names}}" 2>/dev/null | wc -l)
if [ "$consumer_count" -ge 1 ] 2>/dev/null; then
  echo "✓ log-consumer ($consumer_count instance(s) running)"
else
  echo "✗ log-consumer is not running"
fi

echo ""
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo "API Gateway: http://localhost:8080"
echo "Producer: http://localhost:8081"
echo "Consumer: Running internally (no external port)"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo "=========================================="
