package com.example.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

@Component
public class ConsistentHashRing {
    private static final Logger log = LoggerFactory.getLogger(ConsistentHashRing.class);

    private final ConcurrentSkipListMap<Long, String> ring = new ConcurrentSkipListMap<>();
    private final MessageDigest md5;
    private final int virtualNodesPerPhysical;
    private final List<String> physicalNodes;

    public ConsistentHashRing(
            @Value("${index.nodes}") String indexNodesConfig,
            @Value("${ring.virtual.nodes:16}") int virtualNodesPerPhysical) {
        
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
        
        this.virtualNodesPerPhysical = virtualNodesPerPhysical;
        this.physicalNodes = Arrays.asList(indexNodesConfig.split(","));
    }

    @PostConstruct
    public void initialize() {
        log.info("Building consistent hash ring with {} physical nodes, {} virtual nodes each",
                physicalNodes.size(), virtualNodesPerPhysical);

        for (String node : physicalNodes) {
            addNode(node);
        }

        log.info("Hash ring initialized with {} total virtual nodes", ring.size());
    }

    private void addNode(String physicalNode) {
        for (int i = 0; i < virtualNodesPerPhysical; i++) {
            String virtualNode = physicalNode + "#" + i;
            long hash = hash(virtualNode);
            ring.put(hash, physicalNode);
            log.debug("Added virtual node {} -> {} at position {}", virtualNode, physicalNode, hash);
        }
    }

    public String getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        long hash = hash(key);
        
        // Find the first node clockwise from the hash position
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        
        // Wrap around if we've gone past the end of the ring
        if (entry == null) {
            entry = ring.firstEntry();
        }

        String selectedNode = entry.getValue();
        log.debug("Key '{}' (hash={}) routed to node {}", key, hash, selectedNode);
        
        return selectedNode;
    }

    private long hash(String key) {
        md5.reset();
        md5.update(key.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md5.digest();
        
        // Use first 8 bytes as long
        long hash = 0;
        for (int i = 0; i < 8; i++) {
            hash = (hash << 8) | (digest[i] & 0xFF);
        }
        
        return hash;
    }

    public Map<String, Integer> getNodeDistribution(int samples) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (int i = 0; i < samples; i++) {
            String key = "sample_" + i;
            String node = getNode(key);
            distribution.put(node, distribution.getOrDefault(node, 0) + 1);
        }
        
        return distribution;
    }

    public List<String> getPhysicalNodes() {
        return new ArrayList<>(physicalNodes);
    }
}
