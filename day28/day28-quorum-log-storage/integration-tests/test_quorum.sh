#!/bin/bash

# Quorum System Integration Tests

BASE_URL="http://localhost:8080/api/logs"

echo "=== Testing Quorum-Based Log Storage ==="
echo

# Test 1: Write with ONE consistency
echo "Test 1: Write with ONE consistency (fast, low durability)"
curl -X POST "$BASE_URL?consistency=ONE" \
  -H "Content-Type: application/json" \
  -d '{"key":"test-one","value":"data-one"}' \
  -w "\nResponse Time: %{time_total}s\n\n"

sleep 1

# Test 2: Write with QUORUM consistency
echo "Test 2: Write with QUORUM consistency (balanced)"
curl -X POST "$BASE_URL?consistency=QUORUM" \
  -H "Content-Type: application/json" \
  -d '{"key":"test-quorum","value":"data-quorum"}' \
  -w "\nResponse Time: %{time_total}s\n\n"

sleep 1

# Test 3: Write with ALL consistency
echo "Test 3: Write with ALL consistency (slow, high durability)"
curl -X POST "$BASE_URL?consistency=ALL" \
  -H "Content-Type: application/json" \
  -d '{"key":"test-all","value":"data-all"}' \
  -w "\nResponse Time: %{time_total}s\n\n"

sleep 2

# Test 4: Read with ONE consistency
echo "Test 4: Read with ONE consistency (fast, might be stale)"
curl -X GET "$BASE_URL/test-quorum?consistency=ONE" \
  -w "\nResponse Time: %{time_total}s\n\n"

# Test 5: Read with QUORUM consistency
echo "Test 5: Read with QUORUM consistency (balanced)"
curl -X GET "$BASE_URL/test-quorum?consistency=QUORUM" \
  -w "\nResponse Time: %{time_total}s\n\n"

# Test 6: Read with ALL consistency
echo "Test 6: Read with ALL consistency (slowest, most consistent)"
curl -X GET "$BASE_URL/test-quorum?consistency=ALL" \
  -w "\nResponse Time: %{time_total}s\n\n"

echo "=== Tests Complete ==="
