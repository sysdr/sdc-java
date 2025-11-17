package com.example.logprocessor.consumer.service;

import com.example.logprocessor.proto.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class LogStorageService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final Counter storedCounter;
    
    public LogStorageService(RedisTemplate<String, String> redisTemplate,
                            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.storedCounter = Counter.builder("log_events_stored")
            .description("Total log events stored")
            .register(meterRegistry);
    }
    
    public void storeLogEvent(LogEvent logEvent) {
        try {
            // Store in Redis with TTL
            String key = "log:" + logEvent.getEventId();
            String value = formatLogEvent(logEvent);
            
            redisTemplate.opsForValue().set(key, value, Duration.ofHours(24));
            
            // Store recent events list
            redisTemplate.opsForList().leftPush("recent-logs", logEvent.getEventId());
            redisTemplate.opsForList().trim("recent-logs", 0, 999);
            
            storedCounter.increment();
            
            log.debug("Stored log event: {}", logEvent.getEventId());
            
        } catch (Exception e) {
            log.error("Failed to store log event {}: {}", logEvent.getEventId(), e.getMessage());
        }
    }
    
    private String formatLogEvent(LogEvent event) {
        return String.format("[%s] %s - %s: %s (service: %s, trace: %s)",
            event.getTimestamp(),
            event.getLevel(),
            event.getEventId(),
            event.getMessage(),
            event.getServiceName(),
            event.getTraceId());
    }
}
