#!/bin/bash

echo "========================================="
echo "Setting up Log Search System"
echo "========================================="

echo "Starting Docker infrastructure..."
docker-compose up -d

echo "Waiting for services to be ready..."
sleep 30

echo "Checking Elasticsearch health..."
until curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green"\|"status":"yellow"'; do
    echo "Waiting for Elasticsearch..."
    sleep 5
done

echo "Checking Kafka health..."
until docker exec $(docker ps -qf "name=kafka") kafka-topics --list --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    echo "Waiting for Kafka..."
    sleep 5
done

echo ""
echo "========================================="
echo "Infrastructure Ready!"
echo "========================================="
echo "Elasticsearch: http://localhost:9200"
echo "Kibana: http://localhost:5601"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Next steps:"
echo "1. Build services: mvn clean package"
echo "2. Run services in separate terminals:"
echo "   cd log-producer && mvn spring-boot:run"
echo "   cd log-indexer && mvn spring-boot:run"
echo "   cd search-api && mvn spring-boot:run"
echo "3. Run integration tests: ./integration-tests/test-flow.sh"
echo "4. Run load tests: ./load-test.sh"
echo "========================================="
