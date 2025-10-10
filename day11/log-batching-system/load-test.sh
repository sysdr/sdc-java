#!/bin/bash

echo "Starting load test..."
echo "This will generate 10,000 log requests over 60 seconds"
echo ""

START_TIME=$(date +%s)
SUCCESSFUL=0
FAILED=0

for i in {1..10000}
do
    # Generate random log event
    LOG_JSON=$(cat <<JSON
{
  "id": "$(uuidgen)",
  "level": "INFO",
  "service": "load-test",
  "message": "Load test message $i",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "traceId": "$(uuidgen | cut -d'-' -f1)"
}
JSON
)
    
    # Send to shipper
    RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$LOG_JSON" \
        http://localhost:8082/api/logs \
        -o /dev/null)
    
    if [ "$RESPONSE" == "202" ]; then
        ((SUCCESSFUL++))
    else
        ((FAILED++))
    fi
    
    # Progress indicator
    if [ $((i % 100)) -eq 0 ]; then
        echo "Sent $i requests... (Success: $SUCCESSFUL, Failed: $FAILED)"
    fi
    
    # Rate limiting: 166 requests/second
    sleep 0.006
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "Load test completed!"
echo "Duration: ${DURATION}s"
echo "Successful: $SUCCESSFUL"
echo "Failed: $FAILED"
echo "Rate: $((SUCCESSFUL / DURATION)) req/s"
echo ""
echo "Check metrics at: http://localhost:9090"
echo "Check Grafana at: http://localhost:3000 (admin/admin)"
