#!/bin/bash

echo "Starting MapReduce Framework..."

# Build all services
echo "Building Maven projects..."
mvn clean package -DskipTests

# Start infrastructure
echo "Starting Docker containers..."
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 30

# Create Kafka topics
echo "Creating Kafka topics..."
docker-compose exec kafka kafka-topics --create \
  --topic map-tasks \
  --bootstrap-server localhost:9092 \
  --partitions 4 \
  --replication-factor 1

docker-compose exec kafka kafka-topics --create \
  --topic reduce-tasks \
  --bootstrap-server localhost:9092 \
  --partitions 2 \
  --replication-factor 1

docker-compose exec kafka kafka-topics --create \
  --topic task-completions \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

docker-compose exec kafka kafka-topics --create \
  --topic application-logs \
  --bootstrap-server localhost:9092 \
  --partitions 4 \
  --replication-factor 1

echo "
MapReduce Framework is ready!

Services:
- API Gateway: http://localhost:8090
- Coordinator: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

Submit a job:
curl -X POST http://localhost:8090/api/jobs \\
  -H 'Content-Type: application/json' \\
  -d '{
    \"jobName\": \"word-count-test\",
    \"inputTopic\": \"application-logs\",
    \"numMappers\": 4,
    \"numReducers\": 2
  }'

Check job status:
curl http://localhost:8090/api/jobs/{jobId}
"
