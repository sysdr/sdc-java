#!/bin/bash

echo "🚀 Starting Log Search API Infrastructure"
echo "=========================================="

# Start infrastructure
echo "Starting Docker Compose services..."
docker-compose up -d

echo "Waiting for services to be ready..."
sleep 30

# Wait for Elasticsearch
echo "Waiting for Elasticsearch..."
until curl -s http://localhost:9200 > /dev/null; do
    echo "Elasticsearch not ready yet..."
    sleep 5
done

# Create Elasticsearch index template
echo "Creating Elasticsearch index template..."
curl -X PUT "localhost:9200/_index_template/logs-template" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "mappings": {
      "properties": {
        "timestamp": { "type": "date" },
        "service": { 
          "type": "text",
          "fields": {
            "keyword": { "type": "keyword" }
          }
        },
        "level": { "type": "keyword" },
        "message": { "type": "text" },
        "traceId": { "type": "keyword" },
        "hostname": { "type": "keyword" }
      }
    }
  }
}
'

echo ""
echo "✅ Infrastructure ready!"
echo ""
echo "Services:"
echo "  - Elasticsearch: http://localhost:9200"
echo "  - Kafka: localhost:9092"
echo "  - Redis: localhost:6379"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Next steps:"
echo "  1. Build services: cd api-gateway && mvn clean package"
echo "  2. Run gateway: java -jar target/api-gateway-1.0.0.jar"
echo "  3. Run producer: cd ../log-producer && mvn spring-boot:run"
echo "  4. Test API: ./load-test.sh"
