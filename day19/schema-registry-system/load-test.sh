#!/bin/bash

REGISTRY_URL=${1:-http://localhost:8081}
CONCURRENT=${2:-10}
REQUESTS=${3:-1000}

# Sample Avro schema
SCHEMA='{
    "type": "record",
    "name": "TestLog",
    "namespace": "com.example.loadtest",
    "fields": [
        {"name": "timestamp", "type": "long"},
        {"name": "level", "type": "string"},
        {"name": "message", "type": "string"}
    ]
}'

echo "Schema Registry Load Test"
echo "========================="
echo "URL: $REGISTRY_URL"
echo "Concurrent: $CONCURRENT"
echo "Total Requests: $REQUESTS"
echo ""

# Test 1: Register schemas
echo "Test 1: Registering test schemas..."
for i in $(seq 1 5); do
    SUBJECT="loadtest-subject-$i"
    curl -s -X POST "$REGISTRY_URL/subjects/$SUBJECT/versions" \
        -H "Content-Type: application/json" \
        -d "{\"schema\": $(echo $SCHEMA | jq -R .), \"schemaType\": \"AVRO\"}" \
        | jq -r '.id // .message'
done
echo ""

# Test 2: Get schemas by ID
echo "Test 2: Fetching schemas by ID..."
echo "Testing schema ID lookups with ab (Apache Bench)..."

if command -v ab &> /dev/null; then
    ab -n $REQUESTS -c $CONCURRENT "$REGISTRY_URL/schemas/ids/1" 2>&1 | grep -E "Requests per second|Time per request|Failed requests"
else
    echo "Apache Bench (ab) not installed. Using curl loop..."
    start_time=$(date +%s.%N)
    for i in $(seq 1 100); do
        curl -s "$REGISTRY_URL/schemas/ids/1" > /dev/null &
    done
    wait
    end_time=$(date +%s.%N)
    duration=$(echo "$end_time - $start_time" | bc)
    echo "100 requests completed in ${duration}s"
fi

echo ""
echo "Test 3: Listing subjects..."
curl -s "$REGISTRY_URL/subjects" | jq .

echo ""
echo "Load test complete!"
