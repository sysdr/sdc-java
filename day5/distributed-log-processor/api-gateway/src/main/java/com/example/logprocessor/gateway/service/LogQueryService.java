package com.example.logprocessor.gateway.service;

import com.example.logprocessor.gateway.model.LogEvent;
import com.example.logprocessor.gateway.repository.LogEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class LogQueryService {

    private static final Logger logger = LoggerFactory.getLogger(LogQueryService.class);
    private static final String REDIS_CACHE_PREFIX = "query:cache:";

    private final LogEventRepository logEventRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public LogQueryService(LogEventRepository logEventRepository, RedisTemplate<String, String> redisTemplate) {
        this.logEventRepository = logEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public Page<LogEvent> queryLogs(String level, String source, String keyword, 
                                  LocalDateTime startTime, LocalDateTime endTime, 
                                  Pageable pageable) {
        
        String cacheKey = generateCacheKey("query", level, source, keyword, startTime, endTime, pageable);
        
        try {
            // Try cache first for recent queries
            String cachedResult = redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                logger.debug("Cache hit for query: {}", cacheKey);
                // In a real implementation, you'd deserialize the Page object
                // For simplicity, we'll just log the cache hit and proceed with DB query
            }
        } catch (Exception e) {
            logger.warn("Failed to check cache for query: {}", cacheKey, e);
        }

        // Build dynamic query based on parameters
        Page<LogEvent> result;
        
        if (startTime != null && endTime != null) {
            if (level != null && source != null) {
                result = logEventRepository.findByLevelAndSourceAndTimestampBetween(
                    level, source, startTime, endTime, pageable);
            } else if (level != null) {
                result = logEventRepository.findByLevelAndTimestampBetween(level, startTime, endTime, pageable);
            } else if (source != null) {
                result = logEventRepository.findBySourceAndTimestampBetween(source, startTime, endTime, pageable);
            } else {
                result = logEventRepository.findByTimestampBetween(startTime, endTime, pageable);
            }
        } else {
            if (level != null && source != null) {
                result = logEventRepository.findByLevelAndSource(level, source, pageable);
            } else if (level != null) {
                result = logEventRepository.findByLevel(level, pageable);
            } else if (source != null) {
                result = logEventRepository.findBySource(source, pageable);
            } else {
                result = logEventRepository.findAll(pageable);
            }
        }

        // Cache successful queries for 5 minutes
        try {
            // In a real implementation, you'd serialize the entire Page object
            redisTemplate.opsForValue().set(cacheKey, "cached", 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Failed to cache query result: {}", cacheKey, e);
        }

        return result;
    }

    public LogEvent getLogByTraceId(String traceId) {
        // Check hot storage (Redis) first
        try {
            String redisKey = "log:hot:" + traceId;
            String cachedLog = redisTemplate.opsForValue().get(redisKey);
            if (cachedLog != null) {
                logger.debug("Found log in hot storage: trace_id={}", traceId);
                return objectMapper.readValue(cachedLog, LogEvent.class);
            }
        } catch (Exception e) {
            logger.warn("Failed to check hot storage for trace ID: {}", traceId, e);
        }

        // Fallback to warm storage (PostgreSQL)
        LogEvent result = logEventRepository.findByTraceId(traceId);
        if (result != null) {
            logger.debug("Found log in warm storage: trace_id={}", traceId);
        }
        
        return result;
    }

    public Map<String, Object> getLogStatistics(LocalDateTime since) {
        LocalDateTime sinceTime = since != null ? since : LocalDateTime.now().minusHours(24);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Get counts by level
        Map<String, Long> levelCounts = new HashMap<>();
        levelCounts.put("ERROR", logEventRepository.countByLevelAndTimestampAfter("ERROR", sinceTime));
        levelCounts.put("WARN", logEventRepository.countByLevelAndTimestampAfter("WARN", sinceTime));
        levelCounts.put("INFO", logEventRepository.countByLevelAndTimestampAfter("INFO", sinceTime));
        levelCounts.put("DEBUG", logEventRepository.countByLevelAndTimestampAfter("DEBUG", sinceTime));
        
        stats.put("levelCounts", levelCounts);
        stats.put("totalLogs", levelCounts.values().stream().mapToLong(Long::longValue).sum());
        stats.put("since", sinceTime);
        stats.put("generatedAt", LocalDateTime.now());
        
        return stats;
    }

    public List<LogEvent> searchLogs(String query, int limit) {
        // Simple text search implementation
        // In production, you might use Elasticsearch or similar for full-text search
        return logEventRepository.findByMessageContainingIgnoreCase(query, 
            org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    private String generateCacheKey(String prefix, Object... params) {
        StringBuilder key = new StringBuilder(REDIS_CACHE_PREFIX + prefix);
        for (Object param : params) {
            if (param != null) {
                key.append(":").append(param.toString().hashCode());
            }
        }
        return key.toString();
    }
}
