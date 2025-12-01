package com.example.logprocessor.coordinator;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Consistent Hash Ring implementation with virtual nodes.
 * Uses MurmurHash3 for fast, well-distributed hashing.
 */
@Component
@Slf4j
public class ConsistentHashRing {
    
    private final ConcurrentSkipListMap<Long, String> ring = new ConcurrentSkipListMap<>();
    private final int virtualNodesPerNode;
    private final HashFunction hashFunction;
    private final Set<String> physicalNodes = new HashSet<>();
    
    public ConsistentHashRing() {
        this.virtualNodesPerNode = 150; // Balances distribution and performance
        this.hashFunction = Hashing.murmur3_128();
    }
    
    /**
     * Adds a physical node to the ring by creating virtual nodes.
     * Each virtual node is hashed to a different position.
     */
    public synchronized void addNode(String nodeId) {
        if (physicalNodes.contains(nodeId)) {
            log.debug("Node {} already exists in ring", nodeId);
            return;
        }
        
        for (int i = 0; i < virtualNodesPerNode; i++) {
            String virtualNodeKey = nodeId + "-vnode-" + i;
            long hash = hash(virtualNodeKey);
            ring.put(hash, nodeId);
        }
        
        physicalNodes.add(nodeId);
        log.info("Added node {} to ring with {} virtual nodes", nodeId, virtualNodesPerNode);
    }
    
    /**
     * Removes a node and all its virtual nodes from the ring.
     */
    public synchronized void removeNode(String nodeId) {
        if (!physicalNodes.contains(nodeId)) {
            return;
        }
        
        for (int i = 0; i < virtualNodesPerNode; i++) {
            String virtualNodeKey = nodeId + "-vnode-" + i;
            long hash = hash(virtualNodeKey);
            ring.remove(hash);
        }
        
        physicalNodes.remove(nodeId);
        log.info("Removed node {} from ring", nodeId);
    }
    
    /**
     * Gets the node responsible for a given key.
     * Uses clockwise traversal to find the first node >= key's hash.
     */
    public String getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        
        long hash = hash(key);
        
        // Find first node >= hash (clockwise)
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        
        // Wrap around if we reached the end
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        return entry.getValue();
    }
    
    /**
     * Gets N nodes for replication (different physical nodes).
     */
    public List<String> getNodesForReplication(String key, int replicationFactor) {
        if (ring.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> nodes = new LinkedHashSet<>();
        long hash = hash(key);
        
        // Start from the key's hash position and go clockwise
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        Iterator<Map.Entry<Long, String>> iterator = ring.tailMap(entry.getKey()).entrySet().iterator();
        
        // Collect unique physical nodes
        while (nodes.size() < replicationFactor && iterator.hasNext()) {
            nodes.add(iterator.next().getValue());
        }
        
        // Wrap around if needed
        if (nodes.size() < replicationFactor) {
            iterator = ring.entrySet().iterator();
            while (nodes.size() < replicationFactor && iterator.hasNext()) {
                nodes.add(iterator.next().getValue());
            }
        }
        
        return new ArrayList<>(nodes);
    }
    
    public Set<String> getPhysicalNodes() {
        return new HashSet<>(physicalNodes);
    }
    
    public int getVirtualNodeCount() {
        return ring.size();
    }
    
    /**
     * Calculates distribution balance across nodes.
     * Returns standard deviation / mean (lower is better).
     */
    public double calculateBalance(Map<String, Long> logsPerNode) {
        if (logsPerNode.isEmpty()) {
            return 0.0;
        }
        
        double mean = logsPerNode.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        double variance = logsPerNode.values().stream()
            .mapToDouble(count -> Math.pow(count - mean, 2))
            .average()
            .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        return mean > 0 ? (stdDev / mean) * 100 : 0.0; // As percentage
    }
    
    private long hash(String key) {
        return hashFunction.hashString(key, StandardCharsets.UTF_8).asLong();
    }
}
