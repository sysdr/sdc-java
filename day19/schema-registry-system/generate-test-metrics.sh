#!/bin/bash

# Script to generate test metrics for the dashboard
# This will register schemas and perform validations to create metrics

REGISTRY_URL="http://localhost:8081"
GATEWAY_URL="http://localhost:8082"

echo "Generating test metrics..."
echo ""

# Register a test schema
echo "1. Registering test schema..."
curl -s -X POST "${REGISTRY_URL}/subjects/test-logs/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"Log\",\"fields\":[{\"name\":\"message\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"long\"}]}",
    "schemaType": "AVRO"
  }' > /dev/null

sleep 1

# Register another schema version
echo "2. Registering schema version 2..."
curl -s -X POST "${REGISTRY_URL}/subjects/test-logs/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"Log\",\"fields\":[{\"name\":\"message\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"long\"},{\"name\":\"level\",\"type\":\"string\"}]}",
    "schemaType": "AVRO"
  }' > /dev/null

sleep 1

# Try to register incompatible schema (should create compatibility failure)
echo "3. Attempting incompatible schema (will fail)..."
curl -s -X POST "${REGISTRY_URL}/subjects/test-logs/versions" \
  -H "Content-Type: application/json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"Log\",\"fields\":[{\"name\":\"message\",\"type\":\"int\"}]}",
    "schemaType": "AVRO"
  }' > /dev/null

sleep 1

# Get schema ID for validation
SCHEMA_ID=$(curl -s "${REGISTRY_URL}/subjects/test-logs/versions/latest" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)

if [ ! -z "$SCHEMA_ID" ]; then
    echo "4. Performing validations (schema ID: $SCHEMA_ID)..."
    
    # Create a valid Avro message (simplified - in real scenario you'd use Avro serialization)
    # For now, just trigger the validation endpoint
    for i in {1..5}; do
        curl -s -X POST "${GATEWAY_URL}/validate/schema/${SCHEMA_ID}" \
          -H "Content-Type: application/octet-stream" \
          --data-binary "test-message-${i}" > /dev/null
        sleep 0.5
    done
    
    echo "5. Performing more validations..."
    for i in {1..3}; do
        curl -s -X POST "${GATEWAY_URL}/validate" \
          -H "Content-Type: application/octet-stream" \
          --data-binary "test-validation-${i}" > /dev/null
        sleep 0.5
    done
fi

echo ""
echo "✅ Test metrics generated!"
echo ""
echo "Metrics should now be visible in:"
echo "  - Dashboard: http://localhost:8080/dashboard.html"
echo "  - Prometheus: http://localhost:9090"
echo ""
echo "Wait a few seconds for Prometheus to scrape the metrics, then refresh the dashboard."

