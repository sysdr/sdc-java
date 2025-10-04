#!/bin/bash

# Integration test for TCP log server

HOST="localhost"
PORT="9090"

echo "Testing TCP connection to $HOST:$PORT"

# Test 1: Send a valid log message
echo '{"timestamp":"2025-01-15T10:30:00Z","level":"INFO","message":"Test log message","source":"integration-test"}' | nc $HOST $PORT

# Test 2: Send multiple log messages
for i in {1..10}; do
    echo "{\"timestamp\":\"2025-01-15T10:30:0${i}Z\",\"level\":\"INFO\",\"message\":\"Test message $i\",\"source\":\"load-test\"}" | nc $HOST $PORT
done

echo "Integration tests sent successfully"

# Wait for data to be persisted
sleep 2

# Query via REST API
echo "Querying logs via REST API..."
curl -s "http://localhost:8080/api/logs/stats?start=2025-01-15T10:00:00Z&end=2025-01-15T11:00:00Z" | jq .

echo "Integration test complete"
