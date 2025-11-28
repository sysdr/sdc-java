#!/bin/bash

WRITE_URL="http://localhost:9090/api/write"
READ_URL="http://localhost:9091/api/read"
NUM_REQUESTS=100

echo "Running load test with $NUM_REQUESTS requests..."

# Write test
echo "Testing writes..."
start=$(date +%s)
for i in $(seq 1 $NUM_REQUESTS); do
  curl -s -X POST $WRITE_URL \
    -H "Content-Type: application/json" \
    -d "{\"key\":\"log-$i\",\"content\":\"Test log entry $i\"}" \
    > /dev/null &
done
wait
end=$(date +%s)
duration=$((end - start))
if [ $duration -eq 0 ]; then
  duration=1
fi
echo "Wrote $NUM_REQUESTS entries in ${duration}s ($(($NUM_REQUESTS / $duration)) writes/sec)"

# Read test
echo "Testing reads..."
sleep 2
start=$(date +%s)
for i in $(seq 1 $NUM_REQUESTS); do
  curl -s $READ_URL/log-$i > /dev/null &
done
wait
end=$(date +%s)
duration=$((end - start))
if [ $duration -eq 0 ]; then
  duration=1
fi
echo "Read $NUM_REQUESTS entries in ${duration}s ($(($NUM_REQUESTS / $duration)) reads/sec)"

echo "Load test complete!"
