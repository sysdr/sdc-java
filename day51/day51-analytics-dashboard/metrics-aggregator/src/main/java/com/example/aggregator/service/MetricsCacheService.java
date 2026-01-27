package com.example.aggregator.service;

import com.example.aggregator.model.MetricPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages Redis caching of metrics with time-windowed keys.
 * Enables high-performance queries for recent time ranges.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsCacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    
    public void cacheMetric(MetricPoint metric) {
        try {
            // Round timestamp to nearest minute for cache key
            long bucket = metric.getTimestamp().getEpochSecond() / 60;
            String key = String.format("metrics:%s:%d", metric.getMetricName(), bucket);
            
            String value = objectMapper.writeValueAsString(metric);
            redisTemplate.opsForList().rightPush(key, value);
            redisTemplate.expire(key, CACHE_TTL);
        } catch (Exception e) {
            log.error("Error caching metric: {}", e.getMessage());
        }
    }
    
    public List<MetricPoint> getMetrics(String metricName, Instant start, Instant end) {
        List<MetricPoint> results = new ArrayList<>();
        
        try {
            long startBucket = start.getEpochSecond() / 60;
            long endBucket = end.getEpochSecond() / 60;
            
            for (long bucket = startBucket; bucket <= endBucket; bucket++) {
                String key = String.format("metrics:%s:%d", metricName, bucket);
                List<String> values = redisTemplate.opsForList().range(key, 0, -1);
                
                if (values != null) {
                    for (String value : values) {
                        MetricPoint point = objectMapper.readValue(value, MetricPoint.class);
                        if (!point.getTimestamp().isBefore(start) && !point.getTimestamp().isAfter(end)) {
                            results.add(point);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving cached metrics: {}", e.getMessage());
        }
        
        return results;
    }
}
