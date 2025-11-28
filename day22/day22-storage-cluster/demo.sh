#!/bin/bash

echo "Running demo to populate dashboard metrics..."

WRITE_URL="http://localhost:9090/api/write"
READ_URL="http://localhost:9091/api/read"

# Write multiple entries to generate metrics
echo "Writing test data..."
for i in $(seq 1 50); do
  curl -s -X POST $WRITE_URL \
    -H "Content-Type: application/json" \
    -d "{\"key\":\"demo-key-$i\",\"content\":\"Demo content entry $i - $(date +%s)\"}" \
    > /dev/null
  if [ $((i % 10)) -eq 0 ]; then
    echo "Written $i entries..."
  fi
done

echo "Reading test data..."
for i in $(seq 1 50); do
  curl -s $READ_URL/demo-key-$i > /dev/null
  if [ $((i % 10)) -eq 0 ]; then
    echo "Read $i entries..."
  fi
done

echo "Demo complete! Check dashboard at http://localhost:3000"
