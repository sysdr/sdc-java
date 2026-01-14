#!/bin/bash

PRODUCER_URL="http://localhost:8081"
QUERY_URL="http://localhost:8083"

echo "🔥 Running Demo to Populate Dashboard Metrics..."

# Create some test entities
echo "Creating test entities..."
for i in {1..50}; do
    ENTITY_ID="entity-$i"
    ENTITY_TYPE="user"
    STATUS="active"
    
    curl -s -X POST "$PRODUCER_URL/api/state/update" \
        -H "Content-Type: application/json" \
        -d "{\"entityId\":\"$ENTITY_ID\",\"entityType\":\"$ENTITY_TYPE\",\"status\":\"$STATUS\",\"attributes\":{\"index\":$i},\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"version\":1}" > /dev/null
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "Created $i entities..."
    fi
done

echo "Waiting for materialization..."
sleep 10

echo ""
echo "📊 Querying Statistics:"
curl -s "$QUERY_URL/api/query/stats"

echo ""
echo ""
echo "✅ Demo complete! Check Grafana dashboard at http://localhost:3000"
echo "   Login: admin/admin"
