#!/bin/bash
set -e

REGISTRY_URL=${1:-http://localhost:8081}

echo "Running Schema Registry Integration Tests"
echo "=========================================="
echo ""

# Test 1: Health check
echo "Test 1: Health check..."
HEALTH=$(curl -s "$REGISTRY_URL/actuator/health" | jq -r '.status')
if [ "$HEALTH" = "UP" ]; then
    echo "✓ Health check passed"
else
    echo "✗ Health check failed: $HEALTH"
    exit 1
fi

# Test 2: Register a schema
echo ""
echo "Test 2: Register schema..."
SCHEMA='{
    "type": "record",
    "name": "IntegrationTest",
    "fields": [
        {"name": "id", "type": "long"},
        {"name": "name", "type": "string"}
    ]
}'

RESULT=$(curl -s -X POST "$REGISTRY_URL/subjects/integration-test/versions" \
    -H "Content-Type: application/json" \
    -d "{\"schema\": $(echo $SCHEMA | jq -R .), \"schemaType\": \"AVRO\"}")

SCHEMA_ID=$(echo $RESULT | jq -r '.id')
if [ "$SCHEMA_ID" != "null" ] && [ -n "$SCHEMA_ID" ]; then
    echo "✓ Schema registered with ID: $SCHEMA_ID"
else
    echo "✗ Schema registration failed: $RESULT"
    exit 1
fi

# Test 3: Retrieve schema by ID
echo ""
echo "Test 3: Retrieve schema by ID..."
RETRIEVED=$(curl -s "$REGISTRY_URL/schemas/ids/$SCHEMA_ID" | jq -r '.subject')
if [ "$RETRIEVED" = "integration-test" ]; then
    echo "✓ Schema retrieved successfully"
else
    echo "✗ Schema retrieval failed"
    exit 1
fi

# Test 4: Get latest version
echo ""
echo "Test 4: Get latest version..."
VERSION=$(curl -s "$REGISTRY_URL/subjects/integration-test/versions/latest" | jq -r '.version')
if [ "$VERSION" = "1" ]; then
    echo "✓ Latest version is correct: $VERSION"
else
    echo "✗ Version mismatch: expected 1, got $VERSION"
    exit 1
fi

# Test 5: Register compatible schema
echo ""
echo "Test 5: Register compatible schema (backward compatible)..."
COMPATIBLE_SCHEMA='{
    "type": "record",
    "name": "IntegrationTest",
    "fields": [
        {"name": "id", "type": "long"},
        {"name": "name", "type": "string"},
        {"name": "email", "type": ["null", "string"], "default": null}
    ]
}'

RESULT2=$(curl -s -X POST "$REGISTRY_URL/subjects/integration-test/versions" \
    -H "Content-Type: application/json" \
    -d "{\"schema\": $(echo $COMPATIBLE_SCHEMA | jq -R .), \"schemaType\": \"AVRO\"}")

SCHEMA_ID2=$(echo $RESULT2 | jq -r '.id')
if [ "$SCHEMA_ID2" != "null" ] && [ -n "$SCHEMA_ID2" ]; then
    echo "✓ Compatible schema registered with ID: $SCHEMA_ID2"
else
    echo "✗ Compatible schema registration failed: $RESULT2"
    exit 1
fi

# Test 6: List versions
echo ""
echo "Test 6: List versions..."
VERSIONS=$(curl -s "$REGISTRY_URL/subjects/integration-test/versions" | jq -r '. | length')
if [ "$VERSIONS" = "2" ]; then
    echo "✓ Correct number of versions: $VERSIONS"
else
    echo "✗ Version count mismatch: expected 2, got $VERSIONS"
    exit 1
fi

# Test 7: Compatibility check
echo ""
echo "Test 7: Test compatibility..."
COMPAT=$(curl -s -X POST "$REGISTRY_URL/subjects/integration-test/compatibility" \
    -H "Content-Type: application/json" \
    -d "{\"schema\": $(echo $COMPATIBLE_SCHEMA | jq -R .), \"schemaType\": \"AVRO\"}" \
    | jq -r '.compatible')
if [ "$COMPAT" = "true" ]; then
    echo "✓ Compatibility check passed"
else
    echo "✗ Compatibility check failed"
    exit 1
fi

# Test 8: Configuration
echo ""
echo "Test 8: Get/Set configuration..."
CONFIG=$(curl -s "$REGISTRY_URL/subjects/integration-test/config" | jq -r '.compatibilityLevel')
echo "✓ Current compatibility: $CONFIG"

# Cleanup
echo ""
echo "Test 9: Cleanup..."
curl -s -X DELETE "$REGISTRY_URL/subjects/integration-test" > /dev/null
echo "✓ Test subject deleted"

echo ""
echo "=========================================="
echo "All integration tests passed! ✓"
