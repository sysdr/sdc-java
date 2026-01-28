#!/bin/bash

echo "🔥 Starting load test..."

COORDINATOR_URL="http://localhost:8080/api/query"
QUERIES=(
  '{"query": "SELECT * WHERE level = '\''ERROR'\'' LIMIT 100"}'
  '{"query": "SELECT service WHERE level = '\''INFO'\'' LIMIT 50"}'
  '{"query": "SELECT * WHERE service = '\''api-gateway'\'' LIMIT 100"}'
)

# Run 100 queries in parallel
for i in {1..100}; do
  QUERY=${QUERIES[$((RANDOM % 3))]}
  
  (
    START=$(date +%s%N)
    curl -s -X POST "$COORDINATOR_URL" \
      -H "Content-Type: application/json" \
      -d "$QUERY" > /dev/null
    END=$(date +%s%N)
    DURATION=$((($END - $START) / 1000000))
    echo "Query $i completed in ${DURATION}ms"
  ) &
  
  # Stagger requests
  sleep 0.1
done

wait

echo "✅ Load test complete"
