package com.example.statequeryapi.service;

import com.example.statequeryapi.model.EntityState;
import com.example.statequeryapi.model.EntityStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StateQueryService {

    private final EntityStateRepository repository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Counter cacheHitsCounter;
    private final Counter cacheMissesCounter;
    private final Timer queryTimer;

    private static final String CACHE_PREFIX = "entity:";

    public StateQueryService(EntityStateRepository repository,
                            RedisTemplate<String, String> redisTemplate,
                            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.cacheHitsCounter = Counter.builder("state.query.cache.hits")
                .description("Redis cache hits")
                .register(meterRegistry);
        this.cacheMissesCounter = Counter.builder("state.query.cache.misses")
                .description("Redis cache misses (database fallback)")
                .register(meterRegistry);
        this.queryTimer = Timer.builder("state.query.time")
                .description("State query execution time")
                .register(meterRegistry);
    }

    /**
     * Query entity state with Redis cache-aside pattern.
     * Cache hit: <5ms, Cache miss + DB: 20-50ms
     */
    @CircuitBreaker(name = "stateQuery", fallbackMethod = "getEntityStateFallback")
    public Optional<EntityState> getEntityState(String entityId) {
        Timer.Sample sample = Timer.start();
        
        try {
            // Try cache first
            String cacheKey = CACHE_PREFIX + entityId;
            String cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                cacheHitsCounter.increment();
                log.debug("Cache hit for entity: {}", entityId);
                EntityState state = objectMapper.readValue(cached, EntityState.class);
                return Optional.of(state);
            }
            
            // Cache miss: query database
            cacheMissesCounter.increment();
            log.debug("Cache miss for entity: {}, querying database", entityId);
            
            Optional<EntityState> state = repository.findById(entityId);
            
            // Write to cache for future queries
            state.ifPresent(s -> {
                try {
                    String json = objectMapper.writeValueAsString(s);
                    redisTemplate.opsForValue().set(cacheKey, json, Duration.ofHours(24));
                } catch (Exception e) {
                    log.warn("Failed to cache entity state: {}", entityId, e);
                }
            });
            
            return state;
            
        } catch (Exception e) {
            log.error("Error querying entity state: {}", entityId, e);
            throw new RuntimeException("State query failed", e);
        } finally {
            sample.stop(queryTimer);
        }
    }

    public Optional<EntityState> getEntityStateFallback(String entityId, Exception e) {
        log.warn("Circuit breaker fallback for entity: {}", entityId);
        // Return empty on circuit breaker open - client can retry
        return Optional.empty();
    }

    public List<EntityState> getStatesByType(String entityType) {
        log.info("Querying all entities of type: {}", entityType);
        return repository.findByEntityType(entityType);
    }

    public List<EntityState> getStatesByStatus(String status) {
        log.info("Querying all entities with status: {}", status);
        return repository.findByStatus(status);
    }

    public long countByType(String entityType) {
        return repository.countByEntityType(entityType);
    }

    public long getTotalEntityCount() {
        return repository.count();
    }
}
