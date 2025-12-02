#!/bin/bash

echo "🧪 Testing Leader Election..."

# Test 1: Check cluster status
echo "Test 1: Cluster Status"
STATUS=$(curl -s http://localhost:8080/api/status)
echo $STATUS | python3 -m json.tool

LEADER=$(echo $STATUS | python3 -c "import sys, json; print(json.load(sys.stdin)['leader'])")
echo "Current leader: $LEADER"

# Test 2: Write to cluster
echo ""
echo "Test 2: Write Request"
WRITE_RESULT=$(curl -s -X POST http://localhost:8080/api/write \
  -H "Content-Type: application/json" \
  -d '{"data":"Integration test entry"}')
echo $WRITE_RESULT | python3 -m json.tool

# Test 3: Simulate leader failure
echo ""
echo "Test 3: Simulating Leader Failure"
LEADER_PORT=$(echo $LEADER | grep -oP ':\K\d+')
if [ ! -z "$LEADER_PORT" ]; then
  LEADER_CONTAINER=$(docker-compose ps | grep $LEADER_PORT | awk '{print $1}')
  echo "Stopping leader container: $LEADER_CONTAINER"
  docker-compose stop $LEADER_CONTAINER
  
  echo "Waiting for new leader election (5 seconds)..."
  sleep 5
  
  NEW_STATUS=$(curl -s http://localhost:8080/api/status)
  NEW_LEADER=$(echo $NEW_STATUS | python3 -c "import sys, json; print(json.load(sys.stdin)['leader'])")
  echo "New leader: $NEW_LEADER"
  
  # Test write to new leader
  echo "Writing to new leader..."
  curl -s -X POST http://localhost:8080/api/write \
    -H "Content-Type: application/json" \
    -d '{"data":"Post-failover entry"}' | python3 -m json.tool
  
  # Restart old leader
  echo "Restarting old leader..."
  docker-compose start $LEADER_CONTAINER
  sleep 5
fi

echo ""
echo "✅ Integration tests complete"
