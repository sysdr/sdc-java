#!/bin/bash

echo "🧪 Testing Cluster Health System..."

# Test 1: Check cluster coordinator is up
echo "Test 1: Cluster Coordinator Health"
response=$(curl -s http://localhost:8081/actuator/health)
if echo "$response" | grep -q "UP"; then
    echo "✅ Cluster Coordinator is healthy"
else
    echo "❌ Cluster Coordinator health check failed"
    exit 1
fi

# Test 2: Check membership view
echo -e "\nTest 2: Cluster Membership View"
membership=$(curl -s http://localhost:8081/cluster/membership)
if [ ! -z "$membership" ]; then
    echo "✅ Membership view retrieved"
    echo "$membership" | jq '.'
else
    echo "❌ Failed to retrieve membership"
    exit 1
fi

# Test 3: Check cluster status
echo -e "\nTest 3: Cluster Status"
status=$(curl -s http://localhost:8081/cluster/status)
echo "$status" | jq '.'

healthy_nodes=$(echo "$status" | jq '.healthyNodes')
has_quorum=$(echo "$status" | jq '.hasQuorum')

if [ "$has_quorum" = "true" ]; then
    echo "✅ Cluster has quorum ($healthy_nodes healthy nodes)"
else
    echo "⚠️  Cluster does not have quorum"
fi

# Test 4: Verify all services are reporting health
echo -e "\nTest 4: Service Health Checks"
services=("8080:api-gateway" "8081:cluster-coordinator" "8082:log-producer" "8083:log-consumer")

for service in "${services[@]}"; do
    port="${service%%:*}"
    name="${service##*:}"
    
    health=$(curl -s http://localhost:$port/actuator/health 2>/dev/null)
    if echo "$health" | grep -q "UP"; then
        echo "✅ $name is healthy"
    else
        echo "❌ $name is not responding"
    fi
done

echo -e "\n✅ All cluster health tests completed!"
