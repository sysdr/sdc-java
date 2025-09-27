#!/bin/bash

echo "🔬 Running system integration tests..."

# Function to check if service is healthy
check_health() {
  local service_url=$1
  local service_name=$2
  
  if curl -f "$service_url/actuator/health" > /dev/null 2>&1; then
    echo "✅ $service_name is healthy"
    return 0
  else
    echo "❌ $service_name is not healthy"
    return 1
  fi
}

# Test 1: Check all services are healthy
echo "Test 1: Service Health Checks"
check_health "http://localhost:8080" "API Gateway"
check_health "http://localhost:8081" "Log Producer"
check_health "http://localhost:8082" "Log Consumer"

# Test 2: Submit log event and verify it's processed
echo "Test 2: End-to-End Log Processing"
test_log_id=$(uuidgen)
response=$(curl -s -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d "{
    \"id\": \"$test_log_id\",
    \"organizationId\": \"test-org\",
    \"level\": \"INFO\",
    \"message\": \"Integration test message\",
    \"source\": \"integration-test\"
  }")

if echo "$response" | grep -q "success"; then
  echo "✅ Log event submitted successfully"
else
  echo "❌ Failed to submit log event"
  echo "Response: $response"
fi

# Test 3: Check metrics endpoints
echo "Test 3: Metrics Availability"
if curl -f http://localhost:8080/actuator/prometheus > /dev/null 2>&1; then
  echo "✅ API Gateway metrics available"
else
  echo "❌ API Gateway metrics not available"
fi

if curl -f http://localhost:8081/actuator/prometheus > /dev/null 2>&1; then
  echo "✅ Log Producer metrics available"
else
  echo "❌ Log Producer metrics not available"
fi

if curl -f http://localhost:8082/actuator/prometheus > /dev/null 2>&1; then
  echo "✅ Log Consumer metrics available"
else
  echo "❌ Log Consumer metrics not available"
fi

echo "🏁 Integration tests completed!"
