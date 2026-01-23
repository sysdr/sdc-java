package com.example.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WindowQueryService {
    private static final Logger logger = LoggerFactory.getLogger(WindowQueryService.class);
    private static final String REDIS_KEY_PATTERN = "window:*";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final WindowResultRepository repository;
    private final ObjectMapper objectMapper;
    
    public WindowQueryService(RedisTemplate<String, String> redisTemplate,
                             WindowResultRepository repository,
                             ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }
    
    public List<WindowResultDTO> getRecentWindows(int limit) {
        List<WindowResultDTO> results = new ArrayList<>();
        
        // Try Redis first
        try {
            Set<String> keys = redisTemplate.keys(REDIS_KEY_PATTERN);
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    if (results.size() >= limit) break;
                    
                    String json = redisTemplate.opsForValue().get(key);
                    if (json != null) {
                        WindowResultDTO dto = objectMapper.readValue(json, WindowResultDTO.class);
                        results.add(dto);
                    }
                }
                
                if (!results.isEmpty()) {
                    return results.stream()
                        .sorted((a, b) -> Long.compare(b.getWindowStart(), a.getWindowStart()))
                        .limit(limit)
                        .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            logger.warn("Redis query failed, falling back to PostgreSQL: {}", e.getMessage());
        }
        
        // Fallback to PostgreSQL
        Instant oneDayAgo = Instant.now().minusSeconds(86400);
        return repository.findRecent(oneDayAgo).stream()
            .limit(limit)
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    public List<WindowResultDTO> getWindowsByServiceAndTimeRange(
            String service, String windowType, Instant from, Instant to) {
        
        return repository.findByKeyAndTypeAndTimeRange(service, windowType, from, to)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    public WindowStatsDTO getAggregatedStats(Instant from, Instant to) {
        List<WindowResultEntity> windows = repository.findRecent(from);
        
        if (windows.isEmpty()) {
            return WindowStatsDTO.builder()
                .totalEvents(0)
                .totalErrors(0)
                .overallErrorRate(0.0)
                .avgLatencyMs(0.0)
                .maxLatencyMs(0)
                .windowCount(0)
                .build();
        }
        
        long totalEvents = windows.stream().mapToLong(WindowResultEntity::getEventCount).sum();
        long totalErrors = windows.stream().mapToLong(WindowResultEntity::getErrorCount).sum();
        double avgLatency = windows.stream().mapToDouble(WindowResultEntity::getAvgLatencyMs).average().orElse(0.0);
        int maxLatency = windows.stream().mapToInt(WindowResultEntity::getMaxLatencyMs).max().orElse(0);
        
        return WindowStatsDTO.builder()
            .totalEvents(totalEvents)
            .totalErrors(totalErrors)
            .overallErrorRate(totalEvents > 0 ? (double) totalErrors / totalEvents : 0.0)
            .avgLatencyMs(avgLatency)
            .maxLatencyMs(maxLatency)
            .windowCount(windows.size())
            .build();
    }
    
    private WindowResultDTO toDTO(WindowResultEntity entity) {
        return WindowResultDTO.builder()
            .windowKey(entity.getWindowKey())
            .windowStart(entity.getWindowStart().toEpochMilli())
            .windowEnd(entity.getWindowEnd().toEpochMilli())
            .windowType(entity.getWindowType())
            .eventCount(entity.getEventCount())
            .errorCount(entity.getErrorCount())
            .warnCount(entity.getWarnCount())
            .avgLatencyMs(entity.getAvgLatencyMs())
            .maxLatencyMs(entity.getMaxLatencyMs())
            .minLatencyMs(entity.getMinLatencyMs())
            .p95LatencyMs(entity.getP95LatencyMs())
            .errorRate(entity.getErrorRate())
            .computedAt(entity.getComputedAt().toEpochMilli())
            .build();
    }
}
