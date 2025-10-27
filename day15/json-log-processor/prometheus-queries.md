# Prometheus Queries for JSON Log Processing System

## System Metrics

### 1. HTTP Request Rate
Monitor the number of HTTP requests per second
```
rate(http_server_requests_seconds_count[5m])
```

### 2. HTTP Request Latency (95th percentile)
Track response time performance
```
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

### 3. JVM Memory Usage
Monitor Java heap memory
```
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### 4. JVM Thread Count
Track active threads
```
jvm_threads_live_threads
```

### 5. Process CPU Usage
Monitor CPU consumption
```
process_cpu_usage
```

## Application-Specific Metrics

### 6. Log Ingestion Rate (Producer)
Monitor how many logs are being ingested per second
```
rate(log_producer_ingested_total[5m])
```

### 7. Log Processing Rate (Consumer)
Track logs being consumed and processed
```
rate(log_consumer_processed_total[5m])
```

### 8. Kafka Message Lag
Check if consumer is keeping up with producer
```
log_consumer_kafka_message_lag
```

### 9. Schema Validation Success Rate
Monitor validation success percentage
```
rate(schema_validation_success_total[5m]) / rate(schema_validation_total[5m]) * 100
```

### 10. API Gateway Request Rate
Monitor requests through the gateway
```
rate(spring_cloud_gateway_requests_seconds_count[5m])
```

## Service Health

### 11. Service Availability
Check if services are up and responding
```
up{job="api-gateway"} * 100
up{job="log-producer"} * 100
up{job="log-consumer"} * 100
```

### 12. Active Kafka Consumers
Verify consumer group activity
```
kafka_consumer_fetch_manager_records_lag_sum
```

### 13. Database Connection Pool
Monitor PostgreSQL connections
```
hikari_connections_active
hikari_connections_idle
```

### 14. Redis Connection Status
Check Redis connectivity
```
redis_up
```

## Error Monitoring

### 15. HTTP Error Rate
Track 4xx and 5xx errors
```
rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])
```

### 16. Exception Rate
Monitor application exceptions
```
rate(jvm_gc_pause_seconds_count[5m])
```

### 17. Circuit Breaker State
Track Resilience4j circuit breaker status
```
resilience4j_circuitbreaker_state
```

## Performance & Throughput

### 18. Log Throughput (Total)
Overall system throughput
```
rate(log_ingested_total[5m]) + rate(log_processed_total[5m])
```

### 19. Batch Processing Rate
Monitor batch ingestion performance
```
rate(log_batch_processed_total[5m])
```

### 20. End-to-End Latency
Time from ingestion to storage
```
histogram_quantile(0.95, rate(log_processing_duration_seconds_bucket[5m]))
```

## Kafka Metrics

### 21. Kafka Topic Message Rate
Monitor messages produced to Kafka
```
rate(kafka_server_brokertopicmetrics_messages_in_total[5m])
```

### 22. Kafka Consumer Lag
Consumer lag per topic
```
kafka_consumer_lag_sum
```

### 23. Kafka Producer Throughput
Bytes sent per second
```
rate(kafka_producer_record_send_total[5m])
```

## Resource Utilization

### 24. Memory Usage Percentage
Memory consumption across services
```
100 * (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"})
```

### 25. GC Pause Time
Garbage collection impact
```
rate(jvm_gc_pause_seconds_sum[5m])
```

### 26. File Descriptors
Monitor open file descriptors
```
process_files_open
```

## Rate Limiting Metrics

### 27. Rate Limiter State
Monitor rate limiter usage
```
rate(rate_limiter_requests_total[5m])
```

### 28. Rate Limit Exceeded
Track rejected requests due to rate limiting
```
rate(rate_limiter_rejected_total[5m])
```

## Database Metrics

### 29. Query Execution Time
Monitor database query performance
```
histogram_quantile(0.95, rate(hikari_connections_execute_duration_seconds_bucket[5m]))
```

### 30. Active Database Connections
Track connection pool usage
```
hikari_connections_active / hikari_connections_max * 100
```

## Custom Business Metrics

### 31. Log Entries by Level
Count logs by severity level
```
log_level_count{level="INFO"}
log_level_count{level="ERROR"}
log_level_count{level="WARN"}
```

### 32. Service Response Times
Compare response times across services
```
avg(rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])) by (application)
```

### 33. Request Success Rate
Calculate success percentage
```
(rate(http_server_requests_seconds_count{status!~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])) * 100
```

## Useful Alerts

### High Error Rate
```
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 10
```

### High Memory Usage
```
100 * (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 80
```

### Service Down
```
up == 0
```

### High Latency
```
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
```

---

## How to Use in Prometheus

1. Open Prometheus: http://localhost:9090
2. Go to **Graph** tab
3. Paste any query in the query box
4. Click **Execute** to see results
5. Use **Range** selectors to see historical data

## Time Range Modifiers

- `[5m]` - Last 5 minutes
- `[1h]` - Last hour
- `[1d]` - Last day
- `[1w]` - Last week

## Aggregation Functions

- `rate()` - Calculate per-second rate
- `sum()` - Aggregate values
- `avg()` - Calculate average
- `max()` - Get maximum value
- `min()` - Get minimum value
- `by()` - Group by label
