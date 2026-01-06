#!/bin/bash

echo "========================================="
echo "Starting Kafka Cluster Infrastructure"
echo "========================================="

# Start infrastructure
echo "Starting ZooKeeper ensemble and Kafka brokers..."
docker-compose up -d zookeeper-1 zookeeper-2 zookeeper-3

echo "Waiting for ZooKeeper quorum..."
sleep 15

docker-compose up -d kafka-1 kafka-2 kafka-3

echo "Waiting for Kafka brokers to be healthy..."
sleep 30

# Start monitoring
echo "Starting Prometheus and Grafana..."
docker-compose up -d prometheus grafana

# Start application services
echo "Building and starting application services..."
docker-compose up -d kafka-ui kafka-admin-service kafka-health-service load-test-service

echo ""
echo "========================================="
echo "Kafka Cluster Started Successfully!"
echo "========================================="
echo ""
echo "Service URLs:"
echo "  Kafka UI:         http://localhost:8080"
echo "  Admin Service:    http://localhost:8081"
echo "  Health Service:   http://localhost:8082"
echo "  Load Test:        http://localhost:8083"
echo "  Prometheus:       http://localhost:9090"
echo "  Grafana:          http://localhost:3000 (admin/admin)"
echo ""
echo "Kafka Brokers:"
echo "  Broker 1: localhost:19092"
echo "  Broker 2: localhost:19093"
echo "  Broker 3: localhost:19094"
echo ""
echo "View logs: docker-compose logs -f [service-name]"
echo "========================================="
