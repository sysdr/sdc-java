package com.example.logconsumer.service;

import com.example.logconsumer.model.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class LogAggregationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Counter aggregationCounter;

    public LogAggregationService(RedisTemplate<String, String> redisTemplate,
                                MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.aggregationCounter = Counter.builder("log.aggregation.updates")
                .description("Redis aggregation updates")
                .register(meterRegistry);
    }

    public void updateMetrics(LogEvent logEvent) {
        String timeWindow = getTimeWindow(logEvent.getTimestamp());
        
        // Increment counters by service and level
        String serviceKey = String.format("logs:service:%s:%s", logEvent.getService(), timeWindow);
        String levelKey = String.format("logs:level:%s:%s", logEvent.getLevel(), timeWindow);
        
        redisTemplate.opsForValue().increment(serviceKey);
        redisTemplate.opsForValue().increment(levelKey);
        
        // Track error rates for alerting
        if ("ERROR".equals(logEvent.getLevel())) {
            String errorKey = String.format("logs:errors:%s", timeWindow);
            redisTemplate.opsForValue().increment(errorKey);
        }
        
        aggregationCounter.increment();
    }

    private String getTimeWindow(Instant timestamp) {
        // Create 1-minute windows for aggregation
        Instant window = timestamp.truncatedTo(ChronoUnit.MINUTES);
        return String.valueOf(window.getEpochSecond());
    }
}
