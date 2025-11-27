#!/bin/bash

echo "Testing Syslog Ingestion..."

# Test RFC 3164 format via UDP
echo "<34>Oct 11 22:14:15 mymachine su: 'su root' failed for lonvick on /dev/pts/8" | \
    nc -u -w1 localhost 514

# Test RFC 5424 format via TCP
echo "<165>1 2024-01-01T12:00:00Z mymachine myapp 1234 ID47 [exampleSDID@32473 iut=\"3\"] Test message" | \
    nc localhost 601

sleep 2

# Query API Gateway
echo "Querying normalized logs..."
curl -s "http://localhost:8080/api/logs/search?limit=10" | jq '.'

echo "Getting statistics..."
curl -s "http://localhost:8080/api/logs/stats" | jq '.'
