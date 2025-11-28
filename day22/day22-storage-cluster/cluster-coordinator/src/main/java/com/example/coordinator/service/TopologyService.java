package com.example.coordinator.service;

import com.example.coordinator.model.ClusterTopology;
import com.example.coordinator.model.HashRing;
import com.example.coordinator.model.NodeMetadata;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class TopologyService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final HashRing hashRing = new HashRing();
    private final AtomicInteger generationId = new AtomicInteger(0);
    
    private static final Duration FAILURE_THRESHOLD = Duration.ofSeconds(5);
    private String currentLeader = null;
    
    public TopologyService(RedisTemplate<String, Object> redisTemplate,
                          MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        
        // Register metrics
        Gauge.builder("cluster.nodes.total", hashRing, HashRing::size)
            .description("Total nodes in cluster")
            .register(meterRegistry);
        
        Gauge.builder("cluster.generation", generationId, AtomicInteger::get)
            .description("Current cluster generation")
            .register(meterRegistry);
    }
    
    @Scheduled(fixedRateString = "${coordinator.monitor.interval-ms:2000}")
    public void monitorClusterHealth() {
        try {
            Set<String> nodeKeys = redisTemplate.keys("cluster:nodes:*");
            if (nodeKeys == null || nodeKeys.isEmpty()) {
                log.warn("No nodes found in cluster");
                return;
            }
            
            List<NodeMetadata> healthyNodes = new ArrayList<>();
            List<String> failedNodes = new ArrayList<>();
            
            // Check each node's health
            for (String key : nodeKeys) {
                Object nodeObj = redisTemplate.opsForValue().get(key);
                if (nodeObj != null) {
                    NodeMetadata node;
                    // Handle deserialization - could be NodeMetadata, LinkedHashMap, or NodeInfo
                    if (nodeObj instanceof NodeMetadata) {
                        node = (NodeMetadata) nodeObj;
                    } else if (nodeObj instanceof java.util.Map) {
                        // Convert Map to NodeMetadata
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) nodeObj;
                        node = NodeMetadata.builder()
                            .nodeId((String) map.get("nodeId"))
                            .host((String) map.get("host"))
                            .port(((Number) map.get("port")).intValue())
                            .isLeader(Boolean.TRUE.equals(map.get("isLeader")) || Boolean.TRUE.equals(map.get("leader")))
                            .generationId(((Number) map.getOrDefault("generationId", 0)).intValue())
                            .lastHeartbeat(Instant.parse((String) map.get("lastHeartbeat")))
                            .status(NodeMetadata.NodeStatus.valueOf((String) map.get("status")))
                            .build();
                    } else {
                        log.warn("Unexpected node object type: {}", nodeObj.getClass());
                        continue;
                    }
                    
                    Duration timeSinceHeartbeat = Duration.between(
                        node.getLastHeartbeat(), Instant.now()
                    );
                    
                    if (timeSinceHeartbeat.compareTo(FAILURE_THRESHOLD) > 0) {
                        log.warn("Node {} failed - last heartbeat: {}ms ago", 
                                node.getNodeId(), timeSinceHeartbeat.toMillis());
                        failedNodes.add(node.getNodeId());
                        handleNodeFailure(node);
                    } else {
                        healthyNodes.add(node);
                        ensureNodeInRing(node);
                    }
                }
            }
            
            // Update leader if needed
            if (currentLeader == null || failedNodes.contains(currentLeader)) {
                electNewLeader(healthyNodes);
            }
            
            log.debug("Cluster health: {} healthy nodes, {} failed nodes", 
                     healthyNodes.size(), failedNodes.size());
            
        } catch (Exception e) {
            log.error("Error monitoring cluster health", e);
        }
    }
    
    private void ensureNodeInRing(NodeMetadata node) {
        if (hashRing.getNode(node.getNodeId()) == null) {
            hashRing.addNode(node);
            log.info("Added node to hash ring: {}", node.getNodeId());
        }
    }
    
    private void handleNodeFailure(NodeMetadata node) {
        hashRing.removeNode(node.getNodeId());
        redisTemplate.delete("cluster:nodes:" + node.getNodeId());
        log.info("Removed failed node from hash ring: {}", node.getNodeId());
        
        // Publish topology change event
        publishTopologyChange();
    }
    
    private void electNewLeader(List<NodeMetadata> healthyNodes) {
        if (healthyNodes.isEmpty()) {
            log.warn("No healthy nodes available for leader election");
            currentLeader = null;
            return;
        }
        
        // Simple election: pick first healthy node
        NodeMetadata newLeader = healthyNodes.get(0);
        currentLeader = newLeader.getNodeId();
        int newGeneration = generationId.incrementAndGet();
        
        log.info("Elected new leader: {} with generation: {}", currentLeader, newGeneration);
        
        // Update leader metadata
        newLeader.setLeader(true);
        newLeader.setGenerationId(newGeneration);
        redisTemplate.opsForValue().set(
            "cluster:nodes:" + newLeader.getNodeId(), 
            newLeader
        );
        
        // Store leadership info
        redisTemplate.opsForValue().set("cluster:leader", currentLeader);
        redisTemplate.opsForValue().set("cluster:generation", newGeneration);
        
        publishTopologyChange();
    }
    
    private void publishTopologyChange() {
        ClusterTopology topology = ClusterTopology.builder()
            .generationId(generationId.get())
            .leaderId(currentLeader)
            .nodes(new ArrayList<>(hashRing.getAllNodes()))
            .lastUpdate(Instant.now())
            .build();
        
        redisTemplate.opsForValue().set("cluster:topology", topology);
        log.info("Published topology change: generation={}, leader={}, nodes={}", 
                topology.getGenerationId(), topology.getLeaderId(), 
                topology.getNodes().size());
    }
    
    public ClusterTopology getCurrentTopology() {
        // Return current in-memory topology (always up-to-date)
        // The topology stored in Redis may be stale, so we use the hashRing directly
        return ClusterTopology.builder()
            .generationId(generationId.get())
            .leaderId(currentLeader)
            .nodes(new ArrayList<>(hashRing.getAllNodes()))
            .lastUpdate(Instant.now())
            .build();
    }
    
    public List<String> getNodesForKey(String key, int count) {
        return hashRing.getNodesForKey(key, count);
    }
}
