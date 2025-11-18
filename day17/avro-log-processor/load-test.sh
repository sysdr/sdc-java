#!/bin/bash

API_URL="http://localhost:8080/api"
TOTAL_EVENTS=1000
BATCH_SIZE=50
CORRELATION_ID=$(uuidgen)

echo "Load Test: $TOTAL_EVENTS events in batches of $BATCH_SIZE"
echo "Correlation ID: $CORRELATION_ID"
echo ""

start_time=$(date +%s.%N)

for ((i=0; i<TOTAL_EVENTS/BATCH_SIZE; i++)); do
    # Build batch
    BATCH="["
    for ((j=0; j<BATCH_SIZE; j++)); do
        if [ $j -gt 0 ]; then BATCH="$BATCH,"; fi
        EVENT_NUM=$((i*BATCH_SIZE + j))
        BATCH="$BATCH{
            \"level\": \"INFO\",
            \"message\": \"Load test event $EVENT_NUM\",
            \"source\": \"load-tester\",
            \"correlationId\": \"$CORRELATION_ID\",
            \"tags\": {\"batch\": \"$i\", \"event\": \"$EVENT_NUM\"},
            \"schemaVersion\": 2
        }"
    done
    BATCH="$BATCH]"
    
    curl -s -X POST "$API_URL/logs/batch" \
        -H "Content-Type: application/json" \
        -d "$BATCH" > /dev/null
    
    echo -n "."
done

end_time=$(date +%s.%N)
duration=$(echo "$end_time - $start_time" | bc)
rate=$(echo "scale=2; $TOTAL_EVENTS / $duration" | bc)

echo ""
echo ""
echo "Results:"
echo "  Total events:    $TOTAL_EVENTS"
echo "  Duration:        ${duration}s"
echo "  Throughput:      ${rate} events/sec"
echo ""
echo "View metrics at: http://localhost:3000"
