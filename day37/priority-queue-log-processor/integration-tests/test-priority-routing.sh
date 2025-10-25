#!/bin/bash

echo "🧪 Testing Priority Queue Routing..."

# Test 1: Send critical log
echo "Test 1: Critical log routing"
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{"message":"OutOfMemoryError","level":"ERROR","service":"test","httpStatus":500}'

sleep 2

# Test 2: Send normal log
echo "Test 2: Normal log routing"
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -d '{"message":"Request OK","level":"INFO","service":"test","httpStatus":200}'

sleep 2

# Test 3: Check stats
echo "Test 3: Verify processing"
curl http://localhost:8080/api/stats

echo ""
echo "✅ Integration tests completed"
