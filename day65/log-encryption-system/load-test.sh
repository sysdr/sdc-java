#!/bin/bash

# Load test for encryption system - 10,000 events

echo "🚀 Starting load test: 10,000 log events..."

BATCH_SIZE=100
TOTAL_EVENTS=10000
BATCHES=$((TOTAL_EVENTS / BATCH_SIZE))

start_time=$(date +%s)

for i in $(seq 1 $BATCHES); do
  curl -X POST http://localhost:8080/api/logs/bulk \
    -H "Content-Type: application/json" \
    -d "[$(for j in $(seq 1 $BATCH_SIZE); do
      echo '{
        "eventType": "transaction",
        "severity": "INFO",
        "publicFields": {"action": "purchase", "amount": "99.99"},
        "piiFields": {
          "user.email": "user'$i'-'$j'@example.com",
          "payment.cardNumber": "4532-****-****-1234"
        },
        "metadata": {"batch": "'$i'"}
      }'
      if [ $j -lt $BATCH_SIZE ]; then echo ","; fi
    done)]" \
    -s -o /dev/null &
  
  # Send 10 batches in parallel
  if [ $((i % 10)) -eq 0 ]; then
    wait
    echo "Sent $((i * BATCH_SIZE)) events..."
  fi
done

wait

end_time=$(date +%s)
duration=$((end_time - start_time))
throughput=$((TOTAL_EVENTS / duration))

echo "✅ Load test complete!"
echo "📊 Sent: $TOTAL_EVENTS events"
echo "⏱️  Duration: ${duration}s"
echo "🚀 Throughput: ${throughput} events/sec"
