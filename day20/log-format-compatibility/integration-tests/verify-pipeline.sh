#!/bin/bash

echo "Verifying end-to-end pipeline..."

# Check service health
services=("8081" "8082" "8083" "8080")
names=("Syslog Adapter" "Journald Adapter" "Format Normalizer" "API Gateway")

for i in "${!services[@]}"; do
    port="${services[$i]}"
    name="${names[$i]}"
    
    if curl -s "http://localhost:$port/actuator/health" | grep -q "UP"; then
        echo "✓ $name is healthy"
    else
        echo "✗ $name is not responding"
    fi
done

# Check Kafka topics
echo ""
echo "Kafka topics:"
docker exec $(docker ps -qf "name=kafka") kafka-topics --list \
    --bootstrap-server localhost:9092

# Check metrics
echo ""
echo "Checking metrics availability..."
curl -s "http://localhost:8081/actuator/prometheus" | grep "syslog_messages" | head -3
curl -s "http://localhost:8083/actuator/prometheus" | grep "normalizer_events" | head -3

echo ""
echo "Pipeline verification complete!"
