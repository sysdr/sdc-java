package com.example.logprocessor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class HeartbeatMonitor {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final FailoverCoordinator failoverCoordinator;
    
    private static final String HEARTBEAT_KEY = "leader:heartbeat";
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HEARTBEAT_CHECK_INTERVAL = Duration.ofSeconds(2);
    
    public HeartbeatMonitor(
            RedisTemplate<String, String> redisTemplate,
            FailoverCoordinator failoverCoordinator) {
        this.redisTemplate = redisTemplate;
        this.failoverCoordinator = failoverCoordinator;
    }
    
    @Scheduled(fixedRate = 1000) // Every 1 second
    public void updateHeartbeat() {
        if (failoverCoordinator.hasLeadership()) {
            String heartbeatValue = String.format("%d:%d:%s",
                Instant.now().toEpochMilli(),
                failoverCoordinator.getCurrentEpoch(),
                failoverCoordinator.getInstanceId()
            );
            
            redisTemplate.opsForValue().set(HEARTBEAT_KEY, heartbeatValue, Duration.ofSeconds(10));
            log.debug("Updated heartbeat: {}", heartbeatValue);
        }
    }
    
    @Scheduled(fixedRate = 2000) // Every 2 seconds
    public void checkHeartbeat() {
        if (!failoverCoordinator.hasLeadership()) {
            String heartbeat = redisTemplate.opsForValue().get(HEARTBEAT_KEY);
            
            if (heartbeat == null) {
                log.warn("No heartbeat found! Leader may have failed.");
                return;
            }
            
            try {
                String[] parts = heartbeat.split(":");
                long timestamp = Long.parseLong(parts[0]);
                Instant lastHeartbeat = Instant.ofEpochMilli(timestamp);
                Duration age = Duration.between(lastHeartbeat, Instant.now());
                
                if (age.compareTo(HEARTBEAT_TIMEOUT) > 0) {
                    log.error("❌ Heartbeat timeout detected! Age: {}s", age.getSeconds());
                    log.info("Leader failure detected, coordinator will trigger election");
                } else {
                    log.debug("Heartbeat healthy, age: {}ms", age.toMillis());
                }
                
            } catch (Exception e) {
                log.error("Failed to parse heartbeat", e);
            }
        }
    }
}
