#!/bin/bash

set -e

echo "🧪 Running RBAC Integration Tests..."

API_GATEWAY="http://localhost:8080"
AUTH_SERVICE="http://localhost:8081"

# Test 1: Login as admin
echo "Test 1: Admin login..."
ADMIN_RESPONSE=$(curl -s -X POST ${AUTH_SERVICE}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "❌ Failed to get admin token"
  exit 1
fi
echo "✅ Admin login successful"

# Test 2: Query logs as admin (should succeed)
echo "Test 2: Query logs as admin..."
curl -s -X POST ${API_GATEWAY}/api/logs/query \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"query":"ERROR","team":"payments"}' | grep -q "logs"

if [ $? -eq 0 ]; then
  echo "✅ Admin can query logs"
else
  echo "❌ Admin query failed"
  exit 1
fi

# Test 3: Login as developer
echo "Test 3: Developer login..."
DEV_RESPONSE=$(curl -s -X POST ${AUTH_SERVICE}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"developer","password":"dev123"}')

DEV_TOKEN=$(echo $DEV_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$DEV_TOKEN" ]; then
  echo "❌ Failed to get developer token"
  exit 1
fi
echo "✅ Developer login successful"

# Test 4: Developer queries own team (should succeed)
echo "Test 4: Developer queries own team logs..."
curl -s -X POST ${API_GATEWAY}/api/logs/query \
  -H "Authorization: Bearer ${DEV_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"query":"INFO","team":"payments"}' | grep -q "logs"

if [ $? -eq 0 ]; then
  echo "✅ Developer can query own team"
else
  echo "❌ Developer own team query failed"
  exit 1
fi

# Test 5: Developer queries other team (should fail)
echo "Test 5: Developer queries unauthorized team..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${API_GATEWAY}/api/logs/query \
  -H "Authorization: Bearer ${DEV_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"query":"ERROR","team":"security"}')

if [ "$HTTP_CODE" == "403" ]; then
  echo "✅ Developer correctly denied access to other team"
else
  echo "❌ Authorization check failed (got HTTP $HTTP_CODE)"
  exit 1
fi

# Test 6: Invalid token (should fail)
echo "Test 6: Invalid token test..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${API_GATEWAY}/api/logs/query \
  -H "Authorization: Bearer invalid-token-here" \
  -H "Content-Type: application/json" \
  -d '{"query":"ERROR","team":"payments"}')

if [ "$HTTP_CODE" == "401" ]; then
  echo "✅ Invalid token correctly rejected"
else
  echo "❌ Invalid token test failed (got HTTP $HTTP_CODE)"
  exit 1
fi

echo ""
echo "🎉 All RBAC integration tests passed!"
