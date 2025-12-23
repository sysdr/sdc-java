#!/bin/bash

echo "=== Running Performance Load Tests ==="

# Burst test
echo "Executing burst load test..."
curl -X POST http://localhost:8081/api/loadtest/burst \
  -H "Content-Type: application/json" \
  -d '{
    "testName": "burst-test",
    "baselineRatePerSecond": 5000,
    "burstRatePerSecond": 20000,
    "baselineDuration": "PT2M",
    "burstDuration": "PT1M"
  }'

echo ""
echo "Burst test completed. Check results at http://localhost:8081/api/results"

# Ramp test
echo ""
echo "Executing ramp load test..."
curl -X POST http://localhost:8081/api/loadtest/ramp \
  -H "Content-Type: application/json" \
  -d '{
    "testName": "ramp-test",
    "baselineRatePerSecond": 1000,
    "burstRatePerSecond": 15000,
    "rampDuration": "PT10M"
  }'

echo ""
echo "Ramp test completed. Generate performance report:"
echo "curl http://localhost:8080/api/performance/report?durationMinutes=60"
