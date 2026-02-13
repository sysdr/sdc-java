#!/bin/bash

# Demo script to generate traffic and populate dashboard metrics
# This ensures dashboard values are not zero

echo "🎬 Starting RBAC System Demo..."
echo "This script will generate traffic to populate dashboard metrics"
echo ""

API_GATEWAY="http://localhost:8080"
AUTH_SERVICE="http://localhost:8081"

# Wait for services to be ready
echo "Waiting for services to be ready..."
for i in {1..30}; do
  if curl -s ${AUTH_SERVICE}/auth/health > /dev/null 2>&1; then
    echo "✅ Services are ready!"
    break
  fi
  echo "Waiting... ($i/30)"
  sleep 2
done

# Get admin token
echo ""
echo "Step 1: Logging in as admin..."
ADMIN_RESPONSE=$(curl -s -X POST ${AUTH_SERVICE}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "❌ Failed to get admin token"
  exit 1
fi
echo "✅ Admin logged in successfully"

# Get developer token
echo ""
echo "Step 2: Logging in as developer..."
DEV_RESPONSE=$(curl -s -X POST ${AUTH_SERVICE}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"developer","password":"dev123"}')

DEV_TOKEN=$(echo $DEV_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$DEV_TOKEN" ]; then
  echo "❌ Failed to get developer token"
  exit 1
fi
echo "✅ Developer logged in successfully"

# Get analyst token
echo ""
echo "Step 3: Logging in as analyst..."
ANALYST_RESPONSE=$(curl -s -X POST ${AUTH_SERVICE}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst","password":"analyst123"}')

ANALYST_TOKEN=$(echo $ANALYST_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ANALYST_TOKEN" ]; then
  echo "❌ Failed to get analyst token"
  exit 1
fi
echo "✅ Analyst logged in successfully"

# Generate traffic
echo ""
echo "Step 4: Generating traffic to populate metrics..."
echo "  - Sending log queries as admin..."
for i in {1..20}; do
  curl -s -X POST ${API_GATEWAY}/api/logs/query \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"query\":\"ERROR\",\"team\":\"payments\",\"maxResults\":100}" > /dev/null
  sleep 0.1
done

echo "  - Sending log queries as developer..."
for i in {1..15}; do
  curl -s -X POST ${API_GATEWAY}/api/logs/query \
    -H "Authorization: Bearer ${DEV_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"query\":\"INFO\",\"team\":\"payments\",\"maxResults\":50}" > /dev/null
  sleep 0.1
done

echo "  - Testing authorization failures (developer accessing unauthorized team)..."
for i in {1..5}; do
  curl -s -X POST ${API_GATEWAY}/api/logs/query \
    -H "Authorization: Bearer ${DEV_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"query\":\"ERROR\",\"team\":\"security\"}" > /dev/null
  sleep 0.1
done

echo "  - Testing invalid token (should generate 401 errors)..."
for i in {1..3}; do
  curl -s -X POST ${API_GATEWAY}/api/logs/query \
    -H "Authorization: Bearer invalid-token-123" \
    -H "Content-Type: application/json" \
    -d "{\"query\":\"ERROR\",\"team\":\"payments\"}" > /dev/null
  sleep 0.1
done

echo "  - Sending queries as analyst..."
for i in {1..10}; do
  curl -s -X POST ${API_GATEWAY}/api/logs/query \
    -H "Authorization: Bearer ${ANALYST_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"query\":\"INFO\",\"team\":\"analytics\",\"maxResults\":25}" > /dev/null
  sleep 0.1
done

# Additional logins to increase auth metrics
echo ""
echo "Step 5: Generating additional authentication requests..."
for i in {1..10}; do
  curl -s -X POST ${AUTH_SERVICE}/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' > /dev/null
  sleep 0.2
done

echo ""
echo "✅ Demo completed successfully!"
echo ""
echo "Dashboard metrics should now be populated with:"
echo "  - Authentication requests (login attempts)"
echo "  - Log queries executed"
echo "  - Authorization failures (403 errors)"
echo "  - Unauthorized requests (401 errors)"
echo "  - HTTP response status codes"
echo "  - Request latency"
echo ""
echo "View the dashboard at: http://localhost:3000"
echo "  Username: admin"
echo "  Password: admin"
echo ""
echo "Prometheus metrics: http://localhost:9090"
