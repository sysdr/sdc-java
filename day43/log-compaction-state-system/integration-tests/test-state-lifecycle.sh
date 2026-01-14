#!/bin/bash

set -e

PRODUCER_URL="http://localhost:8081"
QUERY_URL="http://localhost:8083"
TEST_ENTITY_ID="test-entity-123"
TEST_ENTITY_TYPE="user"

echo "🧪 Running State Lifecycle Integration Test..."

# Test 1: Create entity state
echo ""
echo "Test 1: Creating entity state..."
CREATE_RESPONSE=$(curl -s -X POST "$PRODUCER_URL/api/state/update" \
    -H "Content-Type: application/json" \
    -d "{
        \"entityId\": \"$TEST_ENTITY_ID\",
        \"entityType\": \"$TEST_ENTITY_TYPE\",
        \"status\": \"active\",
        \"attributes\": {\"test\": \"initial\"},
        \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
        \"version\": 1
    }")

echo "✅ State created: $CREATE_RESPONSE"

# Wait for materialization
echo "⏳ Waiting for state materialization..."
sleep 5

# Test 2: Query entity state
echo ""
echo "Test 2: Querying entity state..."
QUERY_RESPONSE=$(curl -s "$QUERY_URL/api/query/entity/$TEST_ENTITY_ID")
echo "✅ Query result: $QUERY_RESPONSE"

if echo "$QUERY_RESPONSE" | jq -e ".entityId == \"$TEST_ENTITY_ID\"" > /dev/null; then
    echo "✅ Entity state query successful"
else
    echo "❌ Entity state query failed"
    exit 1
fi

# Test 3: Update entity state (creates new version)
echo ""
echo "Test 3: Updating entity state..."
UPDATE_RESPONSE=$(curl -s -X POST "$PRODUCER_URL/api/state/update" \
    -H "Content-Type: application/json" \
    -d "{
        \"entityId\": \"$TEST_ENTITY_ID\",
        \"entityType\": \"$TEST_ENTITY_TYPE\",
        \"status\": \"updated\",
        \"attributes\": {\"test\": \"updated\"},
        \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
        \"version\": 2
    }")

echo "✅ State updated: $UPDATE_RESPONSE"
sleep 5

# Test 4: Verify compaction (only latest version should be queryable)
echo ""
echo "Test 4: Verifying state compaction..."
UPDATED_QUERY=$(curl -s "$QUERY_URL/api/query/entity/$TEST_ENTITY_ID")

if echo "$UPDATED_QUERY" | jq -e ".status == \"updated\"" > /dev/null; then
    echo "✅ Compaction verified: latest state is current"
else
    echo "❌ Compaction verification failed"
    exit 1
fi

# Test 5: Delete entity (tombstone)
echo ""
echo "Test 5: Deleting entity state (tombstone)..."
DELETE_RESPONSE=$(curl -s -X DELETE "$PRODUCER_URL/api/state/$TEST_ENTITY_TYPE/$TEST_ENTITY_ID")
echo "✅ Tombstone sent: $DELETE_RESPONSE"

sleep 5

# Test 6: Verify deletion
echo ""
echo "Test 6: Verifying entity deletion..."
DELETED_QUERY=$(curl -s -o /dev/null -w "%{http_code}" "$QUERY_URL/api/query/entity/$TEST_ENTITY_ID")

if [ "$DELETED_QUERY" == "404" ]; then
    echo "✅ Entity successfully deleted from materialized view"
else
    echo "❌ Entity deletion verification failed (HTTP $DELETED_QUERY)"
    exit 1
fi

echo ""
echo "✅ All integration tests passed!"
echo ""
echo "📊 System Statistics:"
curl -s "$QUERY_URL/api/query/stats" | jq .
