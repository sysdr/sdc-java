#!/bin/bash

echo "🧪 Running integration tests..."

# Test 1: Send log and verify enrichment
echo "Test 1: Basic log enrichment"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "message": "Integration test log",
    "service": "payment-service",
    "source_ip": "192.168.1.100"
  }')

echo "Response: $RESPONSE"
sleep 2

# Test 2: Verify enriched log in Kafka
echo ""
echo "Test 2: Check Kafka topic for enriched logs"
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic enriched-logs-complete \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 5000

# Test 3: Check metrics
echo ""
echo "Test 3: Verify metrics endpoint"
curl -s http://localhost:8081/actuator/metrics/enrichment.attempts | jq .

echo ""
echo "✅ Integration tests complete!"
