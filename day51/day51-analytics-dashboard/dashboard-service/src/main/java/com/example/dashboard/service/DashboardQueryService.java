package com.example.dashboard.service;

import com.example.dashboard.model.MetricPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Three-tier query architecture:
 * 1. In-memory for last 5 minutes (if available from aggregator)
 * 2. Redis cache for last hour
 * 3. PostgreSQL for historical data
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardQueryService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    @CircuitBreaker(name = "dashboard-query", fallbackMethod = "queryFallback")
    public List<MetricPoint> queryMetrics(String metricName, Instant start, Instant end) {
        Duration timeRange = Duration.between(start, end);
        
        // For recent data (< 1 hour), try Redis cache
        if (timeRange.toHours() < 1) {
            List<MetricPoint> cached = queryCachedMetrics(metricName, start, end);
            if (!cached.isEmpty()) {
                log.info("Cache hit for metric: {}", metricName);
                return cached;
            }
        }
        
        // Fallback to database for historical or cache miss
        return queryDatabaseMetrics(metricName, start, end);
    }
    
    private List<MetricPoint> queryCachedMetrics(String metricName, Instant start, Instant end) {
        // Implementation similar to aggregator's cache service
        List<MetricPoint> results = new ArrayList<>();
        
        try {
            long startBucket = start.getEpochSecond() / 60;
            long endBucket = end.getEpochSecond() / 60;
            
            for (long bucket = startBucket; bucket <= endBucket; bucket++) {
                String key = String.format("metrics:%s:%d", metricName, bucket);
                List<String> values = redisTemplate.opsForList().range(key, 0, -1);
                
                if (values != null && !values.isEmpty()) {
                    for (String value : values) {
                        try {
                            MetricPoint point = objectMapper.readValue(value, MetricPoint.class);
                            if (!point.getTimestamp().isBefore(start) && !point.getTimestamp().isAfter(end)) {
                                results.add(point);
                            }
                        } catch (Exception e) {
                            log.debug("Error parsing cached metric: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error querying cache: {}", e.getMessage());
        }
        
        return results;
    }
    
    private List<MetricPoint> queryDatabaseMetrics(String metricName, Instant start, Instant end) {
        try {
            return jdbcTemplate.query(
                "SELECT metric_name, value, timestamp, labels FROM aggregated_metrics " +
                "WHERE metric_name = ? AND timestamp >= ? AND timestamp <= ? " +
                "ORDER BY timestamp",
                (rs, rowNum) -> MetricPoint.builder()
                    .metricName(rs.getString("metric_name"))
                    .value(rs.getDouble("value"))
                    .timestamp(rs.getTimestamp("timestamp").toInstant())
                    .labels(rs.getString("labels"))
                    .build(),
                metricName, start, end
            );
        } catch (Exception e) {
            log.error("Error querying database: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<MetricPoint> queryFallback(String metricName, Instant start, Instant end, Exception e) {
        log.error("Query circuit breaker triggered for {}: {}", metricName, e.getMessage());
        return new ArrayList<>();
    }
}
