# Important Prometheus Queries for DLQ Log System

## DLQ Metrics

### 1. DLQ Message Rate by Error Type
```promql
rate(dlq_messages_total[5m])
```
**Description**: Rate of messages being sent to DLQ, broken down by error type (VALIDATION, TIMEOUT, PROCESSING, UNKNOWN)

### 2. Total DLQ Messages by Error Type
```promql
dlq_messages_total
```
**Description**: Cumulative count of DLQ messages by error type

### 3. DLQ Message Rate (All Types Combined)
```promql
sum(rate(dlq_messages_total[5m]))
```
**Description**: Overall rate of messages going to DLQ

### 4. DLQ Error Distribution Percentage
```promql
sum(rate(dlq_messages_total[5m])) by (type) / sum(rate(producer_messages_published_total[5m])) * 100
```
**Description**: Percentage of published messages ending up in DLQ by error type

## Message Processing Metrics

### 5. Message Processing Rate
```promql
rate(consumer_messages_processed_total[5m])
```
**Description**: Rate of successfully processed messages

### 6. Message Failure Rate
```promql
rate(consumer_messages_failed_total[5m])
```
**Description**: Rate of failed message processing attempts

### 7. Message Publishing Rate
```promql
rate(producer_messages_published_total[5m])
```
**Description**: Rate of messages published to Kafka by the producer

### 8. Success Rate Percentage
```promql
rate(consumer_messages_processed_total[5m]) / (rate(consumer_messages_processed_total[5m]) + rate(consumer_messages_failed_total[5m])) * 100
```
**Description**: Percentage of messages successfully processed

### 9. Failure Rate Percentage
```promql
rate(consumer_messages_failed_total[5m]) / (rate(consumer_messages_processed_total[5m]) + rate(consumer_messages_failed_total[5m])) * 100
```
**Description**: Percentage of messages that failed processing

## Kafka Producer Metrics

### 10. Kafka Producer Record Send Rate
```promql
rate(kafka_producer_record_send_total[5m])
```
**Description**: Rate of records sent by Kafka producer

### 11. Kafka Producer Error Rate
```promql
rate(kafka_producer_record_error_total[5m])
```
**Description**: Rate of producer errors when sending records

### 12. Kafka Producer Retry Rate
```promql
rate(kafka_producer_record_retry_total[5m])
```
**Description**: Rate of record retries by the producer

### 13. Kafka Producer Bytes Sent Rate
```promql
rate(kafka_producer_byte_total[5m])
```
**Description**: Rate of bytes sent by producer

### 14. Kafka Producer Bytes by Topic
```promql
rate(kafka_producer_topic_byte_total[5m])
```
**Description**: Bytes sent per topic (log-events, log-events-retry, log-events-dlq)

## Kafka Consumer Metrics

### 15. Kafka Consumer Records Consumed Rate
```promql
rate(kafka_consumer_records_consumed_total[5m])
```
**Description**: Rate of records consumed from Kafka

### 16. Kafka Consumer Lag
```promql
kafka_consumer_lag_sum
```
**Description**: Total consumer lag across all partitions

### 17. Kafka Consumer Fetch Rate
```promql
rate(kafka_consumer_fetch_total[5m])
```
**Description**: Rate of fetch requests by consumer

### 18. Consumer Group Join Count
```promql
kafka_consumer_coordinator_join_total
```
**Description**: Number of times consumer groups have joined (indicates rebalancing)

## Service Health Metrics

### 19. Service Uptime
```promql
up{job=~"log-producer|log-consumer|api-gateway"}
```
**Description**: Service availability (1 = up, 0 = down)

### 20. HTTP Request Rate
```promql
rate(http_server_requests_seconds_count[5m])
```
**Description**: Rate of HTTP requests to services

### 21. HTTP Error Rate (4xx/5xx)
```promql
rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])
```
**Description**: Rate of HTTP errors

### 22. HTTP Request Latency (P95)
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```
**Description**: 95th percentile request latency

### 23. HTTP Request Latency (P99)
```promql
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```
**Description**: 99th percentile request latency

## JVM Metrics

### 24. JVM Memory Usage
```promql
jvm_memory_used_bytes{area="heap"}
```
**Description**: Current heap memory usage

### 25. JVM Memory Max
```promql
jvm_memory_max_bytes{area="heap"}
```
**Description**: Maximum heap memory available

### 26. JVM Memory Usage Percentage
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```
**Description**: Heap memory usage percentage

### 27. JVM GC Pause Time
```promql
rate(jvm_gc_pause_seconds_sum[5m])
```
**Description**: Rate of garbage collection pause time

### 28. JVM Thread Count
```promql
jvm_threads_live_threads
```
**Description**: Current number of live threads

## Database Metrics

### 29. Database Connection Pool Active
```promql
jdbc_connections_active{name="dataSource"}
```
**Description**: Active database connections

### 30. Database Connection Pool Max
```promql
jdbc_connections_max{name="dataSource"}
```
**Description**: Maximum database connections

### 31. Database Connection Pool Usage
```promql
jdbc_connections_active{name="dataSource"} / jdbc_connections_max{name="dataSource"} * 100
```
**Description**: Database connection pool usage percentage

## System Metrics

### 32. CPU Usage
```promql
rate(process_cpu_usage[5m]) * 100
```
**Description**: CPU usage percentage

### 33. Disk Free Space
```promql
disk_free_bytes
```
**Description**: Available disk space

### 34. Disk Usage Percentage
```promql
(1 - (disk_free_bytes / disk_total_bytes)) * 100
```
**Description**: Disk usage percentage

## Composite/Calculated Metrics

### 35. End-to-End Message Throughput
```promql
rate(producer_messages_published_total[5m]) - rate(consumer_messages_processed_total[5m])
```
**Description**: Difference between published and processed (indicates backlog)

### 36. DLQ to Total Messages Ratio
```promql
sum(rate(dlq_messages_total[5m])) / sum(rate(producer_messages_published_total[5m])) * 100
```
**Description**: Percentage of messages going to DLQ (alert if > 5%)

### 37. Processing Efficiency
```promql
rate(consumer_messages_processed_total[5m]) / rate(producer_messages_published_total[5m]) * 100
```
**Description**: Percentage of published messages successfully processed

### 38. Retry Success Rate
```promql
rate(consumer_messages_processed_total[5m]) / (rate(consumer_messages_processed_total[5m]) + rate(dlq_messages_total[5m])) * 100
```
**Description**: Success rate after retries (excluding immediate DLQ)

### 39. Average Messages per Second (Last Hour)
```promql
avg_over_time(rate(producer_messages_published_total[5m])[1h:1m])
```
**Description**: Average message publishing rate over the last hour

### 40. DLQ Growth Rate
```promql
deriv(dlq_messages_total[1h])
```
**Description**: Rate of change in DLQ message count (positive = growing)

## Alerting Queries

### 41. High DLQ Rate Alert
```promql
sum(rate(dlq_messages_total[5m])) / sum(rate(producer_messages_published_total[5m])) > 0.05
```
**Description**: Alert when DLQ rate exceeds 5% of total messages

### 42. Service Down Alert
```promql
up{job=~"log-producer|log-consumer|api-gateway"} == 0
```
**Description**: Alert when any service is down

### 43. High Consumer Lag Alert
```promql
kafka_consumer_lag_sum > 10000
```
**Description**: Alert when consumer lag exceeds 10,000 messages

### 44. High Memory Usage Alert
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
```
**Description**: Alert when heap memory usage exceeds 85%

### 45. High Error Rate Alert
```promql
rate(consumer_messages_failed_total[5m]) > 100
```
**Description**: Alert when failure rate exceeds 100 messages per second

## Topic-Specific Metrics

### 46. Messages by Topic (Producer)
```promql
rate(kafka_producer_topic_byte_total[5m]) by (topic)
```
**Description**: Bytes sent per topic

### 47. DLQ Topic Activity
```promql
rate(kafka_producer_topic_byte_total{topic="log-events-dlq"}[5m])
```
**Description**: Activity on DLQ topic

### 48. Retry Topic Activity
```promql
rate(kafka_producer_topic_byte_total{topic="log-events-retry"}[5m])
```
**Description**: Activity on retry topic

## Time-Based Analysis

### 49. DLQ Messages Last Hour
```promql
increase(dlq_messages_total[1h])
```
**Description**: Total DLQ messages in the last hour

### 50. Processing Rate Trend (1 hour)
```promql
rate(consumer_messages_processed_total[1h])
```
**Description**: Average processing rate over the last hour

---

## Quick Reference: Most Important Queries

1. **Overall System Health**: `up{job=~"log-producer|log-consumer|api-gateway"}`
2. **DLQ Rate**: `sum(rate(dlq_messages_total[5m]))`
3. **Processing Rate**: `rate(consumer_messages_processed_total[5m])`
4. **Failure Rate**: `rate(consumer_messages_failed_total[5m])`
5. **DLQ Percentage**: `sum(rate(dlq_messages_total[5m])) / sum(rate(producer_messages_published_total[5m])) * 100`
6. **Consumer Lag**: `kafka_consumer_lag_sum`
7. **Memory Usage**: `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100`

## Usage Tips

- Use `[5m]` for short-term monitoring and `[1h]` for longer-term trends
- Combine with `by (type)` or `by (job)` for breakdowns
- Use `histogram_quantile()` for percentile calculations
- Set up alerts for queries 41-45
- Create Grafana dashboards grouping related metrics together

