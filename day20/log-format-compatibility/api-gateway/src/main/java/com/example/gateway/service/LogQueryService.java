package com.example.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LogQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogQueryService.class);
    private final Map<String, Map<String, Object>> recentLogs = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public LogQueryService(ObjectMapper objectMapper, StringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "normalized-logs", groupId = "gateway-group")
    public void consumeNormalizedLogs(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> log = objectMapper.readValue(message, Map.class);
            String id = (String) log.get("id");
            
            // Keep last 10000 logs in memory
            if (recentLogs.size() > 10000) {
                String oldestKey = recentLogs.keySet().iterator().next();
                recentLogs.remove(oldestKey);
            }
            
            recentLogs.put(id, log);
            logger.debug("Stored normalized log: {}", id);
        } catch (Exception e) {
            logger.error("Error consuming normalized log", e);
        }
    }

    public List<Map<String, Object>> searchLogs(String level, String source, 
                                                String hostname, int limit) {
        try {
            return recentLogs.values().stream()
                    .filter(log -> {
                        if (log == null) return false;
                        // Case-insensitive level comparison
                        if (level != null && level.length() > 0) {
                            String logLevel = log.get("level") != null ? 
                                log.get("level").toString() : "";
                            if (!level.equalsIgnoreCase(logLevel)) return false;
                        }
                        // Case-insensitive source comparison
                        if (source != null && source.length() > 0) {
                            String logSource = log.get("source") != null ? 
                                log.get("source").toString() : "";
                            if (!source.equalsIgnoreCase(logSource)) return false;
                        }
                        // Case-insensitive hostname comparison (partial match)
                        if (hostname != null && hostname.length() > 0) {
                            String logHostname = log.get("hostname") != null ? 
                                log.get("hostname").toString() : "";
                            if (!logHostname.toLowerCase().contains(hostname.toLowerCase())) return false;
                        }
                        return true;
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error searching logs", e);
            return Collections.emptyList();
        }
    }

    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) recentLogs.size());
        stats.put("totalLogs", (long) recentLogs.size()); // Keep for backward compatibility
        
        // Count by source
        long syslogCount = recentLogs.values().stream()
                .filter(log -> "syslog".equalsIgnoreCase((String) log.getOrDefault("source", "")))
                .count();
        long journaldCount = recentLogs.values().stream()
                .filter(log -> "journald".equalsIgnoreCase((String) log.getOrDefault("source", "")))
                .count();
        
        stats.put("syslog", syslogCount);
        stats.put("journald", journaldCount);
        stats.put("normalized", (long) recentLogs.size()); // All logs in recentLogs are normalized
        
        // Count by level (for backward compatibility)
        Map<String, Long> byLevel = recentLogs.values().stream()
                .collect(Collectors.groupingBy(
                    log -> (String) log.getOrDefault("level", "UNKNOWN"),
                    Collectors.counting()
                ));
        stats.putAll(byLevel);
        
        return stats;
    }
}
