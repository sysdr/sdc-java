#!/bin/bash

echo "Testing Log Producer API..."

# Test single log ingestion
echo ""
echo "1. Testing single log ingestion..."
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "source": "test-app",
    "level": "INFO",
    "message": "Test log message"
  }' && echo ""

# Test ERROR log (should go to priority partition)
echo ""
echo "2. Testing ERROR log (priority partition)..."
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "source": "test-app",
    "level": "ERROR",
    "message": "Test error message",
    "stackTrace": "java.lang.Exception: Test"
  }' && echo ""

# Test batch ingestion
echo ""
echo "3. Testing batch ingestion..."
curl -X POST http://localhost:8080/api/logs/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "source": "batch-app",
      "level": "INFO",
      "message": "Batch log 1"
    },
    {
      "source": "batch-app",
      "level": "WARN",
      "message": "Batch log 2"
    }
  ]' && echo ""

# Check health
echo ""
echo "4. Checking producer health..."
curl -X GET http://localhost:8080/api/logs/health && echo ""

echo ""
echo "✅ Integration tests complete!"
