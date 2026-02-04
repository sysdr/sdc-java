package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEventEntity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis cache layer with its own circuit breaker.
 *
 * Breaker config: COUNT_BASED, threshold 3.
 * Rationale: Redis failures are almost always total (connection refused).
 * We don't need a time window — 3 failures in a row is enough signal.
 *
 * Fallback: return empty. The caller (PostgresWriteService) will just
 * skip the cache and serve from the database. No data loss, just higher latency.
 */
@Service
public class RedisCacheService {
    private static final Logger LOG = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String KEY_PREFIX = "log-event:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, LogEventEntity> redisTemplate;

    public RedisCacheService(RedisTemplate<String, LogEventEntity> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @CircuitBreaker(name = "redisCache", fallbackMethod = "cachePutFallback")
    public void put(LogEventEntity entity) {
        String key = KEY_PREFIX + entity.getEventId();
        redisTemplate.opsForValue().set(key, entity, TTL);
        LOG.debug("Cached event: {}", entity.getEventId());
    }

    @CircuitBreaker(name = "redisCache", fallbackMethod = "cacheGetFallback")
    public Optional<LogEventEntity> get(String eventId) {
        String key = KEY_PREFIX + eventId;
        LogEventEntity entity = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(entity);
    }

    // --- Fallbacks ---
    private void cachePutFallback(LogEventEntity entity, Throwable ex) {
        LOG.warn("[REDIS-CB] Cache put skipped for {}. Reason: {}", entity.getEventId(), ex.getClass().getSimpleName());
        // No-op: the event will still be persisted to PostgreSQL by the caller.
    }

    private Optional<LogEventEntity> cacheGetFallback(String eventId, Throwable ex) {
        LOG.warn("[REDIS-CB] Cache get skipped for {}. Reason: {}", eventId, ex.getClass().getSimpleName());
        return Optional.empty();  // Caller will fall back to DB read.
    }
}
