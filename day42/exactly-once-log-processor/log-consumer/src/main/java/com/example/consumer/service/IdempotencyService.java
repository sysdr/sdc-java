package com.example.consumer.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    public IdempotencyService(RedisTemplate<String, String> redisTemplate,
                             MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.cacheHitCounter = Counter.builder("idempotency.cache.hit")
                .description("Duplicate events detected")
                .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("idempotency.cache.miss")
                .description("New events processed")
                .register(meterRegistry);
    }

    /**
     * Try to acquire processing lock for event
     * Returns true if this is first time processing, false if duplicate
     */
    public boolean tryAcquire(String eventId, long timestamp) {
        String key = KEY_PREFIX + eventId + ":" + timestamp;
        
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, "processing", TTL);
            
            if (Boolean.TRUE.equals(success)) {
                cacheMissCounter.increment();
                log.debug("Acquired processing lock for event: {}", eventId);
                return true;
            } else {
                cacheHitCounter.increment();
                log.info("Duplicate event detected: {}", eventId);
                return false;
            }
        } catch (Exception e) {
            log.error("Redis error checking idempotency for {}: {}", eventId, e.getMessage());
            // Fail open: allow processing to continue
            return true;
        }
    }

    /**
     * Mark event as successfully processed
     */
    public void markProcessed(String eventId, long timestamp) {
        String key = KEY_PREFIX + eventId + ":" + timestamp;
        try {
            redisTemplate.opsForValue().set(key, "processed", TTL);
            log.debug("Marked event as processed: {}", eventId);
        } catch (Exception e) {
            log.error("Failed to mark event processed: {}", e.getMessage());
        }
    }

    /**
     * Release lock if processing failed
     */
    public void release(String eventId, long timestamp) {
        String key = KEY_PREFIX + eventId + ":" + timestamp;
        try {
            redisTemplate.delete(key);
            log.debug("Released processing lock for event: {}", eventId);
        } catch (Exception e) {
            log.error("Failed to release lock: {}", e.getMessage());
        }
    }
}
