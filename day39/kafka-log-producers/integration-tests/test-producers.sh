#!/bin/bash

echo "Testing Application Log Shipper..."
curl -X POST http://localhost:8080/api/v1/logs \
  -H "Content-Type: application/json" \
  -d '{
    "source": "web-app",
    "level": "INFO",
    "message": "User logged in successfully",
    "serviceId": "auth-service"
  }'

echo -e "\n\nTesting Transaction Log Shipper..."
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "type": "PAYMENT",
    "amount": 99.99,
    "currency": "USD"
  }'

echo -e "\n\nTesting Batch Ingestion..."
curl -X POST http://localhost:8081/api/v1/logs/batch \
  -H "Content-Type: application/json" \
  -d '{
    "logs": [
      {"source": "app-1", "level": "INFO", "message": "Message 1"},
      {"source": "app-2", "level": "WARN", "message": "Message 2"},
      {"source": "app-3", "level": "ERROR", "message": "Message 3"}
    ]
  }'
