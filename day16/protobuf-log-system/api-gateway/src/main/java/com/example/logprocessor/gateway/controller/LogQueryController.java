package com.example.logprocessor.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@Slf4j
public class LogQueryController {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public LogQueryController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<String>> getRecentLogs(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<String> recentIds = redisTemplate.opsForList().range("recent-logs", 0, limit - 1);
            return ResponseEntity.ok(recentIds);
        } catch (Exception e) {
            log.error("Failed to fetch recent logs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/event/{eventId}")
    public ResponseEntity<String> getLogEvent(@PathVariable String eventId) {
        try {
            String key = "log:" + eventId;
            String logData = redisTemplate.opsForValue().get(key);
            
            if (logData != null) {
                return ResponseEntity.ok(logData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Failed to fetch log event {}", eventId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Long totalLogs = redisTemplate.opsForList().size("recent-logs");
            
            return ResponseEntity.ok(Map.of(
                "total_recent_logs", totalLogs != null ? totalLogs : 0,
                "status", "healthy"
            ));
        } catch (Exception e) {
            log.error("Failed to fetch stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
