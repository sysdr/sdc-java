#!/bin/bash

# Load test script for the distributed log processing system

PRODUCER_URL="http://localhost:8081/api/v1/logs"
GATEWAY_URL="http://localhost:8080/api/v1/logs"

echo "Starting load test..."

# Function to send a batch of logs
send_logs() {
  local batch_size=100
  local total_batches=10
  
  for i in $(seq 1 $total_batches); do
    echo "Sending batch $i of $total_batches..."
    
    # Generate batch payload
    BATCH_PAYLOAD="["
    for j in $(seq 1 $batch_size); do
      if [ $j -gt 1 ]; then
        BATCH_PAYLOAD+=","
      fi
      BATCH_PAYLOAD+="{
        \"message\": \"Load test message $i-$j from batch processing\",
        \"level\": \"$(shuf -n1 -e DEBUG INFO WARN ERROR)\",
        \"source\": \"load-test-$(shuf -i 1-5 -n1)\",
        \"metadata\": {
          \"batch\": \"$i\",
          \"sequence\": \"$j\",
          \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\"
        }
      }"
    done
    BATCH_PAYLOAD+="]"
    
    # Send batch
    curl -s -X POST "$PRODUCER_URL/batch" \
      -H "Content-Type: application/json" \
      -d "$BATCH_PAYLOAD" > /dev/null
    
    if [ $? -eq 0 ]; then
      echo "Batch $i sent successfully"
    else
      echo "Failed to send batch $i"
    fi
    
    # Small delay between batches
    sleep 0.1
  done
}

# Function to query logs continuously
query_logs() {
  local query_count=50
  
  for i in $(seq 1 $query_count); do
    echo "Executing query $i of $query_count..."
    
    # Random query parameters
    LEVEL=$(shuf -n1 -e "" "INFO" "ERROR" "WARN")
    SOURCE=$(shuf -n1 -e "" "load-test-1" "load-test-2")
    
    QUERY_URL="$GATEWAY_URL?size=20&page=$((i % 5))"
    if [ ! -z "$LEVEL" ]; then
      QUERY_URL+="&logLevel=$LEVEL"
    fi
    if [ ! -z "$SOURCE" ]; then
      QUERY_URL+="&source=$SOURCE"
    fi
    
    RESPONSE=$(curl -s "$QUERY_URL")
    if [ $? -eq 0 ]; then
      echo "Query $i successful"
    else
      echo "Query $i failed"
    fi
    
    sleep 0.2
  done
}

# Run load test phases
echo "Phase 1: Sending logs..."
send_logs &
SEND_PID=$!

sleep 5

echo "Phase 2: Querying logs..."
query_logs &
QUERY_PID=$!

# Wait for all processes to complete
wait $SEND_PID
wait $QUERY_PID

echo "Load test completed!"
echo ""
echo "Check metrics at:"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000"
