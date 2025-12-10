#!/bin/bash

echo "💥 Simulating Node Failure..."

# Stop a random service to simulate failure
SERVICE="log-producer"

echo "Stopping $SERVICE..."
docker-compose stop $SERVICE

echo "Waiting 20 seconds for failure detection..."
for i in {20..1}; do
    echo -n "$i..."
    sleep 1
done
echo ""

echo "Checking cluster status..."
curl -s http://localhost:8081/cluster/status | jq '.'

echo -e "\nRestarting $SERVICE..."
docker-compose start $SERVICE

echo "Waiting 30 seconds for recovery..."
for i in {30..1}; do
    echo -n "$i..."
    sleep 1
done
echo ""

echo "Final cluster status:"
curl -s http://localhost:8081/cluster/status | jq '.'

echo -e "\n✅ Failure simulation completed!"
