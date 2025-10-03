package com.example.logprocessor.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class LogQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogQueryService.class);
    
    private final LogQueryRepository logQueryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Counter queryCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    public LogQueryService(LogQueryRepository logQueryRepository, 
                          @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
                          MeterRegistry meterRegistry) {
        this.logQueryRepository = logQueryRepository;
        this.redisTemplate = redisTemplate;
        this.queryCounter = Counter.builder("log_queries_total")
                .description("Total number of log queries")
                .register(meterRegistry);
        this.cacheHitCounter = Counter.builder("cache_hits_total")
                .description("Total number of cache hits")
                .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("cache_misses_total")
                .description("Total number of cache misses")
                .register(meterRegistry);
    }

    @Timed(value = "log_query_duration", description = "Time taken to execute log queries")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackQuery")
    public Page<LogEntry> queryLogs(LogQueryRequest request) {
        queryCounter.increment();
        
        String cacheKey = generateCacheKey(request);
        
        // Try cache first (if Redis is available)
        if (redisTemplate != null) {
            @SuppressWarnings("unchecked")
            Page<LogEntry> cachedResult = (Page<LogEntry>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                cacheHitCounter.increment();
                logger.debug("Cache hit for query: {}", cacheKey);
                return cachedResult;
            }
            cacheMissCounter.increment();
            logger.debug("Cache miss for query: {}", cacheKey);
        } else {
            logger.debug("Redis not available, skipping cache for query: {}", cacheKey);
        }
        
        Pageable pageable = PageRequest.of(request.page(), request.size());
        Page<LogEntry> result = logQueryRepository.findWithFilters(
            request.startTime(),
            request.endTime(),
            request.logLevel(),
            request.source(),
            request.keyword(),
            pageable
        );
        
        // Cache the result (if Redis is available)
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Cacheable(value = "log-stats", key = "#startTime + '-' + #endTime")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackStats")
    public Map<String, Object> getLogStatistics(Instant startTime, Instant endTime) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get log level distribution
        List<Object[]> logLevelCounts = logQueryRepository.getLogLevelCounts(startTime, endTime);
        Map<String, Long> levelDistribution = new HashMap<>();
        for (Object[] row : logLevelCounts) {
            levelDistribution.put((String) row[0], (Long) row[1]);
        }
        stats.put("logLevelDistribution", levelDistribution);
        
        // Get top sources
        List<Object[]> topSources = logQueryRepository.getTopSourcesByCount(startTime, endTime);
        Map<String, Long> sourceDistribution = new HashMap<>();
        for (Object[] row : topSources) {
            sourceDistribution.put((String) row[0], (Long) row[1]);
        }
        stats.put("topSources", sourceDistribution);
        
        // Get total count
        Long totalCount = logQueryRepository.getTotalLogCount(startTime, endTime);
        stats.put("totalCount", totalCount);
        
        return stats;
    }

    // Circuit breaker fallback methods
    public Page<LogEntry> fallbackQuery(LogQueryRequest request, Exception ex) {
        logger.error("Database circuit breaker activated for query. Returning cached data if available.", ex);
        return Page.empty();
    }

    public Map<String, Object> fallbackStats(Instant startTime, Instant endTime, Exception ex) {
        logger.error("Database circuit breaker activated for stats. Returning empty stats.", ex);
        return Map.of("error", "Service temporarily unavailable");
    }

    private String generateCacheKey(LogQueryRequest request) {
        return String.format("query:%d:%d:%s:%s:%s:%d:%d",
            request.startTime().getEpochSecond(),
            request.endTime().getEpochSecond(),
            request.logLevel() != null ? request.logLevel() : "null",
            request.source() != null ? request.source() : "null",
            request.keyword() != null ? request.keyword() : "null",
            request.page(),
            request.size());
    }
}
