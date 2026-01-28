#!/bin/bash

set -e

echo "==================================="
echo "Load Testing Inverted Index System"
echo "==================================="

PRODUCER_URL="http://localhost:8081/api/logs/generate"
SEARCH_URL="http://localhost:8083/api/search"

echo ""
echo "Phase 1: Generating initial dataset (10,000 logs)..."
curl -X POST "${PRODUCER_URL}?count=10000"
echo ""

echo "Waiting 30 seconds for indexing..."
sleep 30

echo ""
echo "Phase 2: Running search queries..."
for i in {1..10}; do
  curl -s "${SEARCH_URL}?query=error%20authentication&limit=20" > /dev/null
  echo "Search $i completed"
done

echo ""
echo "==================================="
echo "Load test completed!"
echo "Check Grafana dashboard at http://localhost:3000"
echo "Username: admin, Password: admin"
echo "==================================="
