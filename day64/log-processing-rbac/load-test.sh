#!/bin/bash

echo "🔥 Starting RBAC Load Test..."

# Get admin token first
ADMIN_RESPONSE=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $ADMIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

# Run concurrent requests
echo "Sending 1000 requests with 10 concurrent connections..."

ab -n 1000 -c 10 \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -p <(echo '{"query":"ERROR","team":"payments"}') \
  http://localhost:8080/api/logs/query

echo "Load test complete. Check Grafana at http://localhost:3000"
