package com.example.logprocessor.storage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Registers this node with Redis and maintains heartbeat.
 * TTL-based registration ensures failed nodes are automatically removed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NodeRegistrationService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String NODE_KEY_PREFIX = "ring:node:";
    private static final long TTL_SECONDS = 15;
    private String nodeId;
    
    @PostConstruct
    public void register() {
        nodeId = System.getenv().getOrDefault("NODE_ID", "storage-node-1");
        String key = NODE_KEY_PREFIX + nodeId;
        
        // Initial registration
        redisTemplate.opsForValue().set(key, nodeId, TTL_SECONDS, TimeUnit.SECONDS);
        log.info("Registered node {} with Redis", nodeId);
    }
    
    @Scheduled(fixedRate = 5000) // Heartbeat every 5 seconds
    public void heartbeat() {
        try {
            String key = NODE_KEY_PREFIX + nodeId;
            redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Heartbeat sent for node {}", nodeId);
        } catch (Exception e) {
            log.error("Failed to send heartbeat", e);
        }
    }
    
    @PreDestroy
    public void deregister() {
        String key = NODE_KEY_PREFIX + nodeId;
        redisTemplate.delete(key);
        log.info("Deregistered node {}", nodeId);
    }
}
