#!/bin/bash

set -e

echo "Testing node failover..."

# Write an entry
echo "Writing test entry..."
curl -s -X POST http://localhost:9090/api/write \
  -H "Content-Type: application/json" \
  -d '{"key":"failover-test","content":"Testing failover"}'

# Stop one storage node
echo "Stopping storage-node-1..."
docker-compose stop storage-node-1

sleep 5

# Verify writes still work
echo "Testing write with one node down..."
WRITE_RESPONSE=$(curl -s -X POST http://localhost:9090/api/write \
  -H "Content-Type: application/json" \
  -d '{"key":"failover-test-2","content":"Should still work"}')

if echo "$WRITE_RESPONSE" | grep -q '"success":true'; then
  echo "✓ Write succeeded with one node down"
else
  echo "✗ Write failed with one node down"
  docker-compose start storage-node-1
  exit 1
fi

# Verify reads still work
echo "Testing read with one node down..."
READ_RESPONSE=$(curl -s http://localhost:9091/api/read/failover-test)

if echo "$READ_RESPONSE" | grep -q '"success":true'; then
  echo "✓ Read succeeded with one node down"
else
  echo "✗ Read failed with one node down"
  docker-compose start storage-node-1
  exit 1
fi

# Restart the node
echo "Restarting storage-node-1..."
docker-compose start storage-node-1

sleep 10

echo "✓ Failover test passed"
