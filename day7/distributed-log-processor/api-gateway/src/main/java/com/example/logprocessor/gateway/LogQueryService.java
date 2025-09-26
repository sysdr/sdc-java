package com.example.logprocessor.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LogQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogQueryService.class);
    
    private final LogQueryRepository logQueryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public LogQueryService(LogQueryRepository logQueryRepository, RedisTemplate<String, Object> redisTemplate) {
        this.logQueryRepository = logQueryRepository;
        this.redisTemplate = redisTemplate;
    }
    
    public Page<LogEventEntity> queryLogs(
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            String level, 
            String source, 
            Pageable pageable
    ) {
        logger.debug("Querying logs: start={}, end={}, level={}, source={}", 
                    startTime, endTime, level, source);
        
        if (level != null && source != null) {
            // Both level and source filters
            return logQueryRepository.findByLevelAndTimestampBetween(level, startTime, endTime, pageable);
        } else if (level != null) {
            // Level filter only
            return logQueryRepository.findByLevelAndTimestampBetween(level, startTime, endTime, pageable);
        } else if (source != null) {
            // Source filter only
            return logQueryRepository.findBySourceAndTimestampBetween(source, startTime, endTime, pageable);
        } else {
            // Time range only
            return logQueryRepository.findByTimestampBetween(startTime, endTime, pageable);
        }
    }
    
    public Page<LogEventEntity> searchLogs(
            String searchTerm, 
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            Pageable pageable
    ) {
        logger.debug("Searching logs: term={}, start={}, end={}", searchTerm, startTime, endTime);
        return logQueryRepository.searchByMessage(searchTerm, startTime, endTime, pageable);
    }
    
    @Cacheable(value = "logStats", key = "#since.toString()")
    public Map<String, Object> getLogStats(LocalDateTime since) {
        logger.debug("Getting log statistics since: {}", since);
        
        String cacheKey = "log_stats_" + since.toString();
        
        // Try Redis cache first
        Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStats != null) {
            logger.debug("Returning cached statistics");
            return cachedStats;
        }
        
        // Calculate stats from database
        List<Object[]> levelStats = logQueryRepository.getLogLevelStats(since);
        List<Object[]> sourceStats = logQueryRepository.countBySource();
        long totalLogs = logQueryRepository.count();
        
        Map<String, Long> levelCounts = levelStats.stream()
                .collect(HashMap::new, 
                        (map, row) -> map.put((String) row[0], ((Number) row[1]).longValue()),
                        HashMap::putAll);
        
        Map<String, Long> sourceCounts = sourceStats.stream()
                .collect(HashMap::new,
                        (map, row) -> map.put((String) row[0], ((Number) row[1]).longValue()),
                        HashMap::putAll);
        
        Map<String, Object> stats = Map.of(
                "totalLogs", totalLogs,
                "levelCounts", levelCounts,
                "sourceCounts", sourceCounts,
                "timeRange", Map.of(
                        "since", since.toString(),
                        "until", LocalDateTime.now().toString()
                )
        );
        
        // Cache for 5 minutes
        redisTemplate.opsForValue().set(cacheKey, stats, Duration.ofMinutes(5));
        
        return stats;
    }
    
    public Optional<LogEventEntity> getLogById(String id) {
        logger.debug("Getting log by ID: {}", id);
        
        // Try Redis cache first
        String cacheKey = "log_" + id;
        LogEventEntity cachedLog = (LogEventEntity) redisTemplate.opsForValue().get(cacheKey);
        if (cachedLog != null) {
            logger.debug("Returning cached log: {}", id);
            return Optional.of(cachedLog);
        }
        
        // Fallback to database
        Optional<LogEventEntity> log = logQueryRepository.findById(id);
        log.ifPresent(logEvent -> {
            // Cache for 1 hour
            redisTemplate.opsForValue().set(cacheKey, logEvent, Duration.ofHours(1));
        });
        
        return log;
    }
}
