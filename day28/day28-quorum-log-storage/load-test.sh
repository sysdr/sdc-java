#!/bin/bash

# Load test demonstrating consistency vs latency trade-offs

BASE_URL="http://localhost:8080/api/logs"
REQUESTS_PER_LEVEL=100

echo "=== Quorum Load Test ==="
echo "This test demonstrates latency differences between consistency levels"
echo

# Function to run load test
run_load_test() {
    local consistency=$1
    local requests=$2
    
    echo "Testing $consistency consistency with $requests requests..."
    
    start_time=$(date +%s%N)
    
    for i in $(seq 1 $requests); do
        curl -X POST "$BASE_URL?consistency=$consistency" \
          -H "Content-Type: application/json" \
          -d "{\"key\":\"load-test-$consistency-$i\",\"value\":\"data-$i\"}" \
          -s -o /dev/null &
    done
    
    wait
    
    end_time=$(date +%s%N)
    duration=$((($end_time - $start_time) / 1000000))
    avg_latency=$(($duration / $requests))
    
    echo "  Total time: ${duration}ms"
    echo "  Average latency: ${avg_latency}ms"
    echo "  Throughput: $(($requests * 1000 / $duration)) req/s"
    echo
}

# Test each consistency level
run_load_test "ONE" $REQUESTS_PER_LEVEL
sleep 2

run_load_test "QUORUM" $REQUESTS_PER_LEVEL
sleep 2

run_load_test "ALL" $REQUESTS_PER_LEVEL

echo "=== Load Test Complete ==="
echo
echo "Expected results:"
echo "  ONE:    Fastest (5-10ms avg)"
echo "  QUORUM: Medium (15-30ms avg)"
echo "  ALL:    Slowest (50-100ms avg)"
