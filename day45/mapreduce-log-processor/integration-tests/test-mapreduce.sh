#!/bin/bash

set -e

echo "=== MapReduce Integration Test ==="

# Submit test job
echo "1. Submitting word count job..."
RESPONSE=$(curl -s -X POST http://localhost:8090/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{
    "jobName": "integration-test",
    "inputTopic": "application-logs",
    "numMappers": 4,
    "numReducers": 2
  }')

JOB_ID=$(echo $RESPONSE | grep -o '"jobId":"[^"]*"' | cut -d'"' -f4)
echo "Job ID: $JOB_ID"

# Poll for completion
echo "2. Waiting for job completion..."
MAX_WAIT=300
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
  STATUS=$(curl -s http://localhost:8090/api/jobs/$JOB_ID | \
    grep -o '"status":"[^"]*"' | cut -d'"' -f4)
  
  echo "   Status: $STATUS (${ELAPSED}s elapsed)"
  
  if [ "$STATUS" = "COMPLETED" ]; then
    echo "✓ Job completed successfully!"
    break
  elif [ "$STATUS" = "FAILED" ]; then
    echo "✗ Job failed!"
    exit 1
  fi
  
  sleep 5
  ELAPSED=$((ELAPSED + 5))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
  echo "✗ Job timeout!"
  exit 1
fi

# Verify results
echo "3. Verifying results in PostgreSQL..."
RESULT_COUNT=$(docker-compose exec -T postgres psql -U postgres -d mapreduce -t -c \
  "SELECT COUNT(*) FROM results WHERE job_id = '$JOB_ID';")

echo "   Found $RESULT_COUNT result rows"

if [ $RESULT_COUNT -gt 0 ]; then
  echo "✓ Results verified!"
  
  echo "4. Sample results:"
  docker-compose exec -T postgres psql -U postgres -d mapreduce -c \
    "SELECT result_key, result_value FROM results WHERE job_id = '$JOB_ID' ORDER BY result_value DESC LIMIT 10;"
else
  echo "✗ No results found!"
  exit 1
fi

echo "
=== Integration Test PASSED ==="
