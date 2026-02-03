#!/bin/bash

echo "Publishing demo messages..."
for i in {1..30}; do
    curl -s -X POST http://localhost:8080/api/logs \
        -H "Content-Type: application/json" \
        -d "{\"level\":\"INFO\",\"message\":\"Demo message $i\",\"source\":\"demo\"}" > /dev/null
    sleep 0.1
done
echo "Published 30 messages"

sleep 3

echo ""
echo "Current metrics:"
curl -s http://localhost:8081/actuator/prometheus | grep -E "(messages_processed_total|failover_events_total|leader_status|leader_epoch)" | head -5

echo ""
echo "Consumer status:"
curl -s http://localhost:8081/api/failover/status | jq '.'

echo ""
echo "Database count:"
docker exec day59-active-passive-failover-postgres-1 psql -U postgres -d logprocessor -t -c 'SELECT COUNT(*) FROM log_events;' 2>/dev/null
