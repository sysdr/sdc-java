package com.example.coordinator.model;

import lombok.Data;

import java.security.MessageDigest;
import java.util.*;

@Data
public class HashRing {
    
    private static final int VIRTUAL_NODES_PER_PHYSICAL = 16;
    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final Map<String, NodeMetadata> nodes = new HashMap<>();
    
    public void addNode(NodeMetadata node) {
        nodes.put(node.getNodeId(), node);
        
        // Add virtual nodes
        for (int i = 0; i < VIRTUAL_NODES_PER_PHYSICAL; i++) {
            long hash = hash(node.getNodeId() + "-vnode-" + i);
            ring.put(hash, node.getNodeId());
        }
    }
    
    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        
        // Remove virtual nodes
        ring.entrySet().removeIf(entry -> entry.getValue().equals(nodeId));
    }
    
    public List<String> getNodesForKey(String key, int count) {
        if (ring.isEmpty()) {
            return Collections.emptyList();
        }
        
        long hash = hash(key);
        Set<String> selectedNodes = new LinkedHashSet<>();
        
        // Find nodes clockwise from hash position
        SortedMap<Long, String> tailMap = ring.tailMap(hash);
        Iterator<String> iterator = tailMap.values().iterator();
        
        while (selectedNodes.size() < count && iterator.hasNext()) {
            selectedNodes.add(iterator.next());
        }
        
        // Wrap around if needed
        if (selectedNodes.size() < count) {
            iterator = ring.values().iterator();
            while (selectedNodes.size() < count && iterator.hasNext()) {
                selectedNodes.add(iterator.next());
            }
        }
        
        return new ArrayList<>(selectedNodes);
    }
    
    public NodeMetadata getNode(String nodeId) {
        return nodes.get(nodeId);
    }
    
    public Collection<NodeMetadata> getAllNodes() {
        return nodes.values();
    }
    
    public int size() {
        return nodes.size();
    }
    
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (Exception e) {
            return key.hashCode();
        }
    }
}
