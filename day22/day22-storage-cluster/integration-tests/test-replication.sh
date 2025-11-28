#!/bin/bash

set -e

echo "Testing replication..."

# Write a test entry
echo "Writing test entry..."
WRITE_RESPONSE=$(curl -s -X POST http://localhost:9090/api/write \
  -H "Content-Type: application/json" \
  -d '{"key":"replication-test","content":"This should replicate"}')

echo "Write response: $WRITE_RESPONSE"

# Wait for replication
sleep 2

# Read from read gateway (should use quorum read)
echo "Reading with quorum..."
READ_RESPONSE=$(curl -s http://localhost:9091/api/read/replication-test)
echo "Read response: $READ_RESPONSE"

# Verify success
if echo "$READ_RESPONSE" | grep -q '"success":true'; then
  echo "✓ Replication test passed"
else
  echo "✗ Replication test failed"
  exit 1
fi
