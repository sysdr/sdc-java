#!/bin/bash

echo "Running load tests for Log Normalization Service..."

BASE_URL="http://localhost:8081/api/v1/normalize"

# Test JSON normalization
echo ""
echo "Testing JSON to AVRO conversion..."
for i in {1..100}; do
    curl -s -X POST "${BASE_URL}/json?targetFormat=AVRO" \
        -H "Content-Type: application/json" \
        -d '{
            "id": "test-'$i'",
            "timestamp": '$(date +%s000)',
            "level": "INFO",
            "service": "test-service",
            "host": "test-host",
            "message": "Test message '$i'"
        }' > /dev/null
done
echo "Completed 100 JSON to AVRO conversions"

# Test TEXT normalization  
echo ""
echo "Testing TEXT to JSON conversion..."
for i in {1..100}; do
    curl -s -X POST "${BASE_URL}/text?targetFormat=JSON" \
        -H "Content-Type: text/plain" \
        -d "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ) [INFO] [test-service] Test message $i requestId=abc$i duration=100ms" > /dev/null
done
echo "Completed 100 TEXT to JSON conversions"

# Test batch normalization
echo ""
echo "Testing batch normalization..."
curl -s -X POST "${BASE_URL}/batch" \
    -H "Content-Type: application/json" \
    -d '{
        "logs": [
            {"id": "1", "data": "'$(echo '{"level":"INFO","message":"test1"}' | base64)'", "format": "JSON"},
            {"id": "2", "data": "'$(echo '{"level":"WARN","message":"test2"}' | base64)'", "format": "JSON"},
            {"id": "3", "data": "'$(echo '{"level":"ERROR","message":"test3"}' | base64)'", "format": "JSON"}
        ],
        "targetFormat": "AVRO"
    }' | jq .stats

# Generate load via producer
echo ""
echo "Generating mixed-format load via producer..."
curl -s -X POST "http://localhost:8082/api/v1/producer/send?count=500&format=JSON" | jq .
curl -s -X POST "http://localhost:8082/api/v1/producer/send?count=500&format=TEXT" | jq .

echo ""
echo "Load test complete. Check metrics at http://localhost:9090"
