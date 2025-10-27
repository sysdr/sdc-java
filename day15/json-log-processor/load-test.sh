#!/bin/bash

echo "📈 Load Testing JSON Log Processing System"
echo "============================================"

BASE_URL="http://localhost:8080/api/logs/ingest"
DURATION=60  # seconds
RATE=100     # requests per second

echo "Configuration:"
echo "  Target URL: $BASE_URL"
echo "  Duration: ${DURATION}s"
echo "  Target Rate: ${RATE} req/s"
echo ""

# Check if ab (Apache Bench) is installed
if ! command -v ab &> /dev/null; then
    echo "Apache Bench (ab) not found. Installing..."
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        sudo apt-get install -y apache2-utils
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "Apache Bench should be pre-installed on macOS"
    fi
fi

# Create test payload file
cat > /tmp/log_payload.json << PAYLOAD
{
  "level": "INFO",
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")",
  "message": "Load test log message with some content to simulate real logs",
  "service": "load-test",
  "metadata": {
    "test_id": "load-test-001",
    "environment": "test"
  }
}
PAYLOAD

echo "🚀 Starting load test..."
echo ""

# Calculate total requests
TOTAL_REQUESTS=$((DURATION * RATE))

# Run load test
ab -n $TOTAL_REQUESTS \
   -c 10 \
   -p /tmp/log_payload.json \
   -T "application/json" \
   -g /tmp/load_test_results.tsv \
   $BASE_URL

echo ""
echo "============================================"
echo "Load test complete!"
echo "Results saved to: /tmp/load_test_results.tsv"
echo ""
echo "Check metrics at:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "============================================"

# Cleanup
rm /tmp/log_payload.json
