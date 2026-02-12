package com.example.logprocessor.service;

import com.example.logprocessor.model.LogEvent;
import com.example.logprocessor.repository.LogEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class LogPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(LogPersistenceService.class);
    
    private final LogEventRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    public LogPersistenceService(LogEventRepository repository, RedisTemplate<String, Object> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @CircuitBreaker(name = "postgres", fallbackMethod = "persistToCacheFallback")
    public void persistLog(LogEvent event) {
        event.setProcessedAt(Instant.now());
        repository.save(event);
        logger.debug("Persisted log to PostgreSQL: {}", event.getId());
        
        // Cache the log event
        cacheLog(event);
    }

    @CircuitBreaker(name = "redis", fallbackMethod = "skipCache")
    public void cacheLog(LogEvent event) {
        String key = "log:" + event.getId();
        redisTemplate.opsForValue().set(key, event, 1, TimeUnit.HOURS);
        logger.debug("Cached log event: {}", event.getId());
    }

    /**
     * Fallback when PostgreSQL circuit is open
     * Cache-only persistence for graceful degradation
     */
    public void persistToCacheFallback(LogEvent event, Exception ex) {
        logger.warn("PostgreSQL circuit OPEN - persisting to cache only: {}", event.getId());
        try {
            String key = "dlq:log:" + event.getId();
            redisTemplate.opsForValue().set(key, event, 24, TimeUnit.HOURS);
        } catch (Exception cacheEx) {
            logger.error("CRITICAL: Both PostgreSQL and Redis failed for: {}", event.getId(), cacheEx);
            // In production: write to local disk or DLQ
        }
    }

    /**
     * Fallback when Redis circuit is open
     */
    public void skipCache(LogEvent event, Exception ex) {
        logger.warn("Redis circuit OPEN - skipping cache: {}", event.getId());
        // Continue without cache
    }
}
