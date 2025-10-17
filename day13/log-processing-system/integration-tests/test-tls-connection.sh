#!/bin/bash

echo "🧪 Testing TLS-secured log processing system..."

GATEWAY_URL="https://localhost:8080"
PRODUCER_URL="https://localhost:8081"

# Test 1: Gateway health check
echo "Test 1: Gateway health check..."
curl -k ${GATEWAY_URL}/api/v1/health || echo "❌ Gateway health check failed"

# Test 2: Send log via gateway
echo "Test 2: Sending log event via gateway..."
curl -k -X POST ${GATEWAY_URL}/api/v1/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "message": "TLS test log event",
    "source": "integration-test",
    "metadata": {"test": true}
  }' || echo "❌ Log ingestion failed"

# Test 3: Direct producer access
echo "Test 3: Direct producer access..."
curl -k -X POST ${PRODUCER_URL}/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR",
    "message": "Direct TLS test",
    "source": "integration-test",
    "metadata": {"direct": true}
  }' || echo "❌ Direct producer access failed"

# Test 4: Certificate validation
echo "Test 4: Validating certificates..."
echo | openssl s_client -connect localhost:8080 -showcerts 2>/dev/null | \
  openssl x509 -noout -dates || echo "❌ Certificate validation failed"

echo "✅ Integration tests complete!"
