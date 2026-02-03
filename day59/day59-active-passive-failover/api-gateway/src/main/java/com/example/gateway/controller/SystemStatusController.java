package com.example.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@Slf4j
public class SystemStatusController {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public SystemStatusController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @GetMapping("/status")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        String leaderInstance = redisTemplate.opsForValue().get("leader:instance");
        String leaderEpoch = redisTemplate.opsForValue().get("leader:epoch");
        String heartbeat = redisTemplate.opsForValue().get("leader:heartbeat");
        
        status.put("currentLeader", leaderInstance);
        status.put("leaderEpoch", leaderEpoch != null ? Long.parseLong(leaderEpoch) : 0);
        
        if (heartbeat != null) {
            try {
                String[] parts = heartbeat.split(":");
                long timestamp = Long.parseLong(parts[0]);
                Instant lastHeartbeat = Instant.ofEpochMilli(timestamp);
                Duration age = Duration.between(lastHeartbeat, Instant.now());
                
                status.put("lastHeartbeat", lastHeartbeat.toString());
                status.put("heartbeatAgeSeconds", age.getSeconds());
                status.put("heartbeatHealthy", age.getSeconds() < 5);
            } catch (Exception e) {
                log.error("Failed to parse heartbeat", e);
            }
        }
        
        return status;
    }
}
