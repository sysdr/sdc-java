#!/bin/bash

echo "🚀 Generating test data for dashboard..."
echo "This will send logs continuously and show metrics updates"
echo ""

# Send logs in background
(
    for i in {1..100}; do
        curl -s -X POST http://localhost:8080/api/logs \
            -H "Content-Type: application/json" \
            -d "{\"level\":\"INFO\",\"message\":\"Dashboard test log $i\",\"service\":\"dashboard-test\",\"source_ip\":\"192.168.1.$((i % 255))\"}" > /dev/null
        sleep 0.5
    done
) &

# Monitor metrics
for i in {1..20}; do
    sleep 2
    echo "=== Check $i ==="
    curl -s http://localhost:8081/actuator/prometheus | grep "^enrichment" | head -3
    echo ""
done

wait
echo "✅ Test data generation complete!"

