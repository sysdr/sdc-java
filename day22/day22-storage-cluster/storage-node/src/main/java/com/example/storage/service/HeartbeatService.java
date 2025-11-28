package com.example.storage.service;

import com.example.storage.model.NodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class HeartbeatService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${storage.node-id}")
    private String nodeId;
    
    @Value("${server.port}")
    private int port;
    
    @Value("${storage.heartbeat.ttl-seconds:5}")
    private long heartbeatTtlSeconds;
    
    private boolean isLeader = false;
    private int generationId = 0;
    
    public HeartbeatService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Scheduled(fixedRateString = "${storage.heartbeat.interval-ms:1000}")
    public void sendHeartbeat() {
        try {
            NodeInfo nodeInfo = NodeInfo.builder()
                .nodeId(nodeId)
                .host("localhost")
                .port(port)
                .isLeader(isLeader)
                .generationId(generationId)
                .lastHeartbeat(Instant.now())
                .status(NodeInfo.NodeStatus.HEALTHY)
                .build();
            
            String key = "cluster:nodes:" + nodeId;
            redisTemplate.opsForValue().set(key, nodeInfo, heartbeatTtlSeconds, TimeUnit.SECONDS);
            
            log.debug("Heartbeat sent: {}", nodeId);
        } catch (Exception e) {
            log.error("Failed to send heartbeat", e);
        }
    }
    
    public void setLeader(boolean leader, int generation) {
        this.isLeader = leader;
        this.generationId = generation;
        log.info("Node {} leadership changed: isLeader={}, generation={}", 
                 nodeId, isLeader, generationId);
    }
}
