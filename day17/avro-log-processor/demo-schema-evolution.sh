#!/bin/bash

set -e

API_URL="http://localhost:8080/api"

echo "=== Avro Schema Evolution Demonstration ==="
echo ""

# Phase 1: Produce V1 events
echo "Phase 1: Producing V1 schema events..."
for i in {1..5}; do
    curl -s -X POST "$API_URL/logs" \
        -H "Content-Type: application/json" \
        -d '{
            "level": "INFO",
            "message": "V1 event number '"$i"'",
            "source": "demo-producer",
            "schemaVersion": 1
        }' | (command -v jq >/dev/null && jq . || cat)
    sleep 0.5
done

echo ""
echo "Phase 2: Producing V2 schema events with tracing..."
CORRELATION_ID=$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid)

for i in {1..5}; do
    curl -s -X POST "$API_URL/logs" \
        -H "Content-Type: application/json" \
        -d '{
            "level": "INFO",
            "message": "V2 event number '"$i"'",
            "source": "demo-producer",
            "correlationId": "'"$CORRELATION_ID"'",
            "tags": {"environment": "demo", "version": "2"},
            "spanId": "span-'"$i"'",
            "schemaVersion": 2
        }' | (command -v jq >/dev/null && jq . || cat)
    sleep 0.5
done

echo ""
echo "Phase 3: Querying by correlation ID..."
sleep 2
curl -s "$API_URL/correlation/$CORRELATION_ID" | (command -v jq >/dev/null && jq . || cat)

echo ""
echo "Phase 4: Checking Schema Registry..."
echo "Registered subjects:"
curl -s http://localhost:8085/subjects | (command -v jq >/dev/null && jq . || cat)

echo ""
echo "Schema versions for avro-log-events-value:"
curl -s http://localhost:8085/subjects/avro-log-events-value/versions | (command -v jq >/dev/null && jq . || cat)

echo ""
echo "=== Schema Evolution Demo Complete ==="
