package com.example.logprocessor.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final Timer rateLimitTimer;
    
    @Autowired
    public RateLimitingService(RedisTemplate<String, String> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.rateLimitTimer = Timer.builder("rate_limit_check")
                .description("Time taken to check rate limits")
                .register(meterRegistry);
    }
    
    /**
     * Sliding window rate limiting using Redis sorted sets
     * @param key Rate limit key (e.g., "generator:instance1")
     * @param windowSizeSeconds Size of the rate limiting window
     * @param maxRequests Maximum requests allowed in the window
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String key, long windowSizeSeconds, long maxRequests) {
        try {
            return rateLimitTimer.recordCallable(() -> {
            try {
                long now = Instant.now().getEpochSecond();
                long windowStart = now - windowSizeSeconds;
                
                // Remove expired entries
                redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
                
                // Count current entries
                Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, now);
                
                if (currentCount < maxRequests) {
                    // Add current request
                    redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
                    redisTemplate.expire(key, windowSizeSeconds + 1, TimeUnit.SECONDS);
                    return true;
                }
                
                return false;
            } catch (Exception e) {
                // Fallback to allow request if Redis is unavailable
                return true;
            }
        });
        } catch (Exception e) {
            // Fallback to allow request if timing fails
            return true;
        }
    }
    
    /**
     * Get current rate for a key
     */
    public long getCurrentRate(String key, long windowSizeSeconds) {
        try {
            long now = Instant.now().getEpochSecond();
            long windowStart = now - windowSizeSeconds;
            return redisTemplate.opsForZSet().count(key, windowStart, now);
        } catch (Exception e) {
            return 0L;
        }
    }
}
