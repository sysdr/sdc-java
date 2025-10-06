#!/bin/bash

set -e

echo "🧪 Running Integration Tests for TCP Log Shipping System"

BASE_URL="http://localhost:8081"

echo "1. Testing health endpoints..."
curl -f ${BASE_URL}/actuator/health || exit 1
echo "✅ Health check passed"

echo "2. Sending single log event..."
curl -X POST ${BASE_URL}/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "message": "Test log message",
    "service": "integration-test"
  }' || exit 1
echo "✅ Single log sent"

echo "3. Sending batch log events..."
curl -X POST ${BASE_URL}/api/logs/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "level": "INFO",
      "message": "Batch test 1",
      "service": "integration-test"
    },
    {
      "level": "ERROR",
      "message": "Batch test 2",
      "service": "integration-test"
    }
  ]' || exit 1
echo "✅ Batch logs sent"

echo "4. Checking metrics..."
curl -f ${BASE_URL}/actuator/prometheus | grep logs_sent_total || exit 1
echo "✅ Metrics available"

echo ""
echo "🎉 All integration tests passed!"
