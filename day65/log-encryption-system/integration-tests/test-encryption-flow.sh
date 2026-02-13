#!/bin/bash

# Integration test for field-level encryption flow

set -e

echo "🧪 Testing Field-Level Encryption Flow..."

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 30

# Test 1: Ingest log with PII
echo "📝 Test 1: Ingesting log event with PII..."
curl -X POST http://localhost:8080/api/logs/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "user_login",
    "severity": "INFO",
    "publicFields": {
      "action": "login",
      "success": "true",
      "ip": "192.168.1.100"
    },
    "piiFields": {
      "user.email": "john.doe@example.com",
      "user.ssn": "123-45-6789",
      "user.phone": "+1-555-0123"
    },
    "metadata": {
      "source": "web-app",
      "version": "1.0.0"
    }
  }'

echo -e "\n✅ Log event ingested"

# Wait for processing
sleep 5

# Test 2: Query as ADMIN (should see all fields)
echo -e "\n📊 Test 2: Querying as ADMIN..."
ADMIN_RESULT=$(curl -s http://localhost:8083/api/query/logs?eventType=user_login \
  -H "X-User-Id: admin-001" \
  -H "X-User-Role: ADMIN" \
  -H "X-User-Email: admin@example.com")

echo "Admin result: $ADMIN_RESULT"

if echo "$ADMIN_RESULT" | grep -q "john.doe@example.com"; then
  echo "✅ ADMIN can see email"
else
  echo "❌ ADMIN cannot see email"
  exit 1
fi

# Test 3: Query as ANALYST (should see redacted SSN)
echo -e "\n📊 Test 3: Querying as ANALYST..."
ANALYST_RESULT=$(curl -s http://localhost:8083/api/query/logs?eventType=user_login \
  -H "X-User-Id: analyst-001" \
  -H "X-User-Role: ANALYST" \
  -H "X-User-Email: analyst@example.com")

echo "Analyst result: $ANALYST_RESULT"

if echo "$ANALYST_RESULT" | grep -q "REDACTED"; then
  echo "✅ ANALYST sees redacted fields"
else
  echo "❌ ANALYST should see redacted fields"
  exit 1
fi

echo -e "\n✅ All integration tests passed!"
