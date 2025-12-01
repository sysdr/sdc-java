package com.example.logprocessor.coordinator;

import com.example.logprocessor.common.NodeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Manages ring membership by watching Redis for node heartbeats.
 * Nodes register themselves with TTL-based keys.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RingMembershipService {
    
    private final ConsistentHashRing hashRing;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String NODE_KEY_PREFIX = "ring:node:";
    private static final String NODE_INFO_PREFIX = "ring:info:";
    
    /**
     * Polls Redis every 10 seconds to update ring membership.
     * Adds new nodes and removes nodes whose TTL expired.
     */
    @Scheduled(fixedRate = 10000)
    public void refreshRing() {
        try {
            Set<String> currentNodes = getCurrentNodesFromRedis();
            Set<String> ringNodes = hashRing.getPhysicalNodes();
            
            // Add new nodes
            for (String nodeId : currentNodes) {
                if (!ringNodes.contains(nodeId)) {
                    hashRing.addNode(nodeId);
                    log.info("Added new node to ring: {}", nodeId);
                }
            }
            
            // Remove departed nodes
            for (String nodeId : ringNodes) {
                if (!currentNodes.contains(nodeId)) {
                    hashRing.removeNode(nodeId);
                    log.warn("Removed failed node from ring: {}", nodeId);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to refresh ring membership", e);
        }
    }
    
    private Set<String> getCurrentNodesFromRedis() {
        Set<String> nodes = new HashSet<>();
        Set<String> keys = redisTemplate.keys(NODE_KEY_PREFIX + "*");
        
        if (keys != null) {
            for (String key : keys) {
                String nodeId = key.substring(NODE_KEY_PREFIX.length());
                nodes.add(nodeId);
            }
        }
        
        return nodes;
    }
    
    /**
     * Gets detailed information about all nodes in the ring.
     */
    public List<NodeInfo> getNodeInfoList() {
        List<NodeInfo> infoList = new ArrayList<>();
        Set<String> nodes = hashRing.getPhysicalNodes();
        
        for (String nodeId : nodes) {
            String infoKey = NODE_INFO_PREFIX + nodeId;
            NodeInfo info = NodeInfo.builder()
                .nodeId(nodeId)
                .status("ACTIVE")
                .lastHeartbeat(Instant.now())
                .build();
            infoList.add(info);
        }
        
        return infoList;
    }
}
