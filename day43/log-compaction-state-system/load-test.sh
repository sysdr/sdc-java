#!/bin/bash

PRODUCER_URL="http://localhost:8081"
QUERY_URL="http://localhost:8083"
ENTITY_TYPES=("user" "device" "session" "order")
STATUSES=("active" "inactive" "pending" "completed")

echo "🔥 Starting Log Compaction State Management Load Test..."
echo "Generating 10,000 entity state updates..."

for i in {1..10000}; do
    ENTITY_ID="entity-$((i % 1000))"  # 1000 unique entities, multiple updates each
    ENTITY_TYPE="${ENTITY_TYPES[$((RANDOM % 4))]}"
    STATUS="${STATUSES[$((RANDOM % 4))]}"
    
    JSON_PAYLOAD=$(cat <<JSON
{
  "entityId": "$ENTITY_ID",
  "entityType": "$ENTITY_TYPE",
  "status": "$STATUS",
  "attributes": {
    "updateCount": $i,
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  },
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "version": $i
}
JSON
)
    
    curl -s -X POST "$PRODUCER_URL/api/state/update" \
        -H "Content-Type: application/json" \
        -d "$JSON_PAYLOAD" > /dev/null
    
    if [ $((i % 1000)) -eq 0 ]; then
        echo "Sent $i state updates..."
    fi
done

echo ""
echo "✅ Load test complete!"
echo ""
echo "💭 Testing tombstone (deletion)..."
curl -X DELETE "$PRODUCER_URL/api/state/user/entity-500"

sleep 5

echo ""
echo "🔍 Querying state statistics..."
curl -s "$QUERY_URL/api/query/stats" | jq .

echo ""
echo "🔍 Sample entity state query:"
curl -s "$QUERY_URL/api/query/entity/entity-100" | jq .

echo ""
echo "📊 Check Grafana dashboard at http://localhost:3000"
echo "   View compaction metrics, cache hit rates, and query latency"
