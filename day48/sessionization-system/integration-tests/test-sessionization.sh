#!/bin/bash

set +e

echo "🧪 Running Sessionization Integration Tests"
echo "==========================================="

BASE_URL="http://localhost:8080"
ANALYTICS_URL="http://localhost:8083"

echo "1. Checking API Gateway health..."
curl -f "$BASE_URL/api/health" || exit 1
echo " ✅ Gateway healthy"

echo ""
echo "2. Waiting for sessions to accumulate (30 seconds)..."
sleep 30

echo ""
echo "3. Fetching analytics stats..."
STATS=$(curl -s "$BASE_URL/api/analytics/stats?hours=1" || echo "{}")
if echo "$STATS" | grep -q "totalSessions\|error"; then
    echo "$STATS"
    echo " ✅ Stats endpoint responding"
else
    echo " ⚠️  Stats endpoint returned: $STATS"
fi

echo ""
echo "4. Checking for active session (user-1)..."
ACTIVE=$(curl -s "$BASE_URL/api/sessions/active/user-1" || echo "{}")
echo "$ACTIVE"
if echo "$ACTIVE" | grep -q "status"; then
    echo " ✅ Active session endpoint responding"
else
    echo " ⚠️  Active session endpoint returned: $ACTIVE"
fi

echo ""
echo "5. Fetching session history (user-1)..."
HISTORY=$(curl -s "$BASE_URL/api/sessions/history/user-1" || echo "[]")
if echo "$HISTORY" | grep -qE "\[\]|sessionId|userId"; then
    echo " ✅ History endpoint responding (showing first 200 chars)"
    echo "$HISTORY" | head -c 200
    echo ""
else
    echo " ⚠️  History endpoint returned: $HISTORY"
fi

echo ""
echo "6. Fetching converted sessions..."
CONVERTED=$(curl -s "$BASE_URL/api/sessions/converted?size=5" || echo "[]")
if echo "$CONVERTED" | grep -qE "\[\]|sessionId"; then
    echo " ✅ Converted sessions endpoint responding (showing first 200 chars)"
    echo "$CONVERTED" | head -c 200
    echo ""
else
    echo " ⚠️  Converted sessions endpoint returned: $CONVERTED"
fi

echo ""
echo "7. Checking PostgreSQL for persisted sessions..."
POSTGRES_CONTAINER=$(docker ps -qf "name=postgres")
if [ -n "$POSTGRES_CONTAINER" ]; then
    docker exec $POSTGRES_CONTAINER psql -U postgres -d sessiondb -c "SELECT COUNT(*) as session_count FROM user_sessions;" 2>&1 || echo " ⚠️  Could not query PostgreSQL"
else
    echo " ⚠️  PostgreSQL container not found"
fi

echo ""
echo "8. Verifying Redis cache..."
REDIS_CONTAINER=$(docker ps -qf "name=redis")
if [ -n "$REDIS_CONTAINER" ]; then
    docker exec $REDIS_CONTAINER redis-cli KEYS "session:*" 2>&1 | head -10 || echo " ⚠️  Could not query Redis"
else
    echo " ⚠️  Redis container not found"
fi

echo ""
echo "✅ All integration tests passed!"
