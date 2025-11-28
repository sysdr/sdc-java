#!/bin/bash

echo "🧪 Testing Partition Performance..."

# Test 1: Verify partitions exist
echo "Test 1: Checking partitions..."
PARTITION_COUNT=$(docker compose exec -T postgres psql -U loguser -d logdb -t -c "
  SELECT COUNT(*) FROM pg_tables 
  WHERE schemaname = 'public' AND tablename LIKE 'logs_%'
")

echo "Found $PARTITION_COUNT partitions"

# Test 2: Insert test data
echo ""
echo "Test 2: Inserting test logs..."
for i in {1..1000}; do
  curl -s -X POST http://localhost:8081/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"source\": \"test-service\",
      \"message\": \"Performance test $i\",
      \"level\": \"INFO\"
    }" > /dev/null
done

sleep 5

# Test 3: Query with source filter (should prune to few partitions)
echo ""
echo "Test 3: Testing partition-pruned query..."
START_TIME=$(date -u +"%Y-%m-%dT00:00:00")
END_TIME=$(date -u +"%Y-%m-%dT23:59:59")

RESULT=$(curl -s -X POST http://localhost:8084/api/query/logs \
  -H "Content-Type: application/json" \
  -d "{
    \"startTime\": \"$START_TIME\",
    \"endTime\": \"$END_TIME\",
    \"source\": \"test-service\",
    \"limit\": 10
  }")

COUNT=$(echo $RESULT | jq 'length')
echo "Query returned $COUNT results"

# Test 4: Verify partition distribution
echo ""
echo "Test 4: Checking partition size distribution..."
docker compose exec -T postgres psql -U loguser -d logdb -c "
  SELECT 
    schemaname || '.' || tablename AS partition,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) AS size
  FROM pg_tables
  WHERE schemaname = 'public' AND tablename LIKE 'logs_2024%'
  ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC
  LIMIT 10
"

echo ""
echo "✅ Integration tests complete"
