#!/bin/bash

set -e

echo "=== Dashboard Metrics Validation ==="
echo ""

echo "1. Index Node Metrics (Documents Indexed):"
echo "   Node 1:"
curl -s http://localhost:8081/actuator/prometheus | grep "^index_documents_indexed_total" | head -1
echo "   Node 2:"
curl -s http://localhost:8082/actuator/prometheus | grep "^index_documents_indexed_total" | head -1
echo "   Node 3:"
curl -s http://localhost:8083/actuator/prometheus | grep "^index_documents_indexed_total" | head -1
echo ""

echo "2. Log Producer Metrics:"
curl -s http://localhost:8095/actuator/prometheus | grep "^producer_logs_generated_total" | head -1
echo ""

echo "3. Router Metrics:"
curl -s http://localhost:8090/actuator/prometheus | grep "^router_requests_routed_total" | head -1
echo ""

echo "4. Coordinator Metrics:"
curl -s http://localhost:8080/actuator/prometheus | grep "^coordinator_queries_total_total" | head -1
echo ""

echo "5. Running Demo Query:"
RESULT=$(curl -s "http://localhost:8080/api/search?q=error&limit=5")
echo "$RESULT" | jq -r '"Total Hits: " + (.totalHits | tostring) + ", Search Time: " + (.searchTimeMs | tostring) + "ms, Shards: " + (.shardsSucceeded | tostring) + "/" + (.shardsQueried | tostring)'
echo ""

echo "6. Sending 10 demo logs..."
for i in {1..10}; do
  curl -s -X POST http://localhost:8090/api/route \
    -H "Content-Type: application/json" \
    -d "{\"logId\":\"demo_$i\",\"tenantId\":\"tenant_1\",\"timestamp\":$(date +%s)000,\"level\":\"INFO\",\"message\":\"Dashboard demo log $i\",\"service\":\"demo-service\"}" > /dev/null
done
echo "Sent 10 demo logs"
sleep 8
echo ""

echo "7. Checking Updated Metrics:"
echo "   Node 1 documents:"
curl -s http://localhost:8081/actuator/prometheus | grep "^index_documents_indexed_total" | head -1 | awk '{print $2}'
echo "   Router requests:"
curl -s http://localhost:8090/actuator/prometheus | grep "^router_requests_routed_total" | head -1 | awk '{print $2}'
echo ""

echo "=== Validation Complete ==="
echo "All metrics should show non-zero values"
echo "Grafana Dashboard: http://localhost:3000 (admin/admin)"
echo "Prometheus: http://localhost:9090"
