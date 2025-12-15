package com.logprocessor.merkle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class MerkleTreeService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${storage.nodes}")
    private String storageNodes;
    
    @Value("${merkle.tree.depth:10}")
    private int treeDepth;
    
    @Value("${merkle.segment.size:1000}")
    private int segmentSize;
    
    public MerkleTreeService(RedisTemplate<String, String> redisTemplate, WebClient.Builder webClientBuilder) {
        this.redisTemplate = redisTemplate;
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }
    
    public MerkleNode buildTree(String partitionId, String nodeUrl) {
        try {
            // Fetch data range from storage node
            String response = webClient.get()
                .uri(nodeUrl + "/api/storage/read/" + partitionId + "/range?startVersion=1&endVersion=10000")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            List<Map<String, Object>> entries = objectMapper.readValue(response, List.class);
            
            if (entries.isEmpty()) {
                return new MerkleNode(calculateHash(""));
            }
            
            // Build leaf nodes (segments)
            List<MerkleNode> leaves = new ArrayList<>();
            for (int i = 0; i < entries.size(); i += segmentSize) {
                int end = Math.min(i + segmentSize, entries.size());
                List<Map<String, Object>> segment = entries.subList(i, end);
                
                // Hash segment data
                StringBuilder segmentData = new StringBuilder();
                for (Map<String, Object> entry : segment) {
                    segmentData.append(entry.get("checksum")).append(",");
                }
                
                MerkleNode leaf = new MerkleNode(calculateHash(segmentData.toString()));
                leaf.setStartVersion(((Number) segment.get(0).get("version")).longValue());
                leaf.setEndVersion(((Number) segment.get(segment.size() - 1).get("version")).longValue());
                leaf.setPartitionId(partitionId);
                leaves.add(leaf);
            }
            
            // Build tree bottom-up
            MerkleNode root = buildTreeRecursive(leaves);
            
            // Cache in Redis
            String cacheKey = "merkle:" + partitionId + ":" + extractNodeId(nodeUrl);
            redisTemplate.opsForValue().set(cacheKey, 
                objectMapper.writeValueAsString(serializeTree(root)), 
                10, TimeUnit.MINUTES);
            
            return root;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Merkle tree", e);
        }
    }
    
    private MerkleNode buildTreeRecursive(List<MerkleNode> nodes) {
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        
        List<MerkleNode> parents = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i += 2) {
            MerkleNode left = nodes.get(i);
            MerkleNode right = (i + 1 < nodes.size()) ? nodes.get(i + 1) : left;
            
            String combinedHash = calculateHash(left.getHash() + right.getHash());
            MerkleNode parent = new MerkleNode(left, right, combinedHash);
            parents.add(parent);
        }
        
        return buildTreeRecursive(parents);
    }
    
    public List<InconsistentSegment> compareTree(String partitionId, String node1Url, String node2Url) {
        try {
            MerkleNode tree1 = getOrBuildTree(partitionId, node1Url);
            MerkleNode tree2 = getOrBuildTree(partitionId, node2Url);
            
            List<InconsistentSegment> inconsistencies = new ArrayList<>();
            compareNodes(tree1, tree2, inconsistencies);
            
            return inconsistencies;
        } catch (Exception e) {
            throw new RuntimeException("Failed to compare trees", e);
        }
    }
    
    private void compareNodes(MerkleNode node1, MerkleNode node2, List<InconsistentSegment> inconsistencies) {
        if (node1.getHash().equals(node2.getHash())) {
            return; // Subtrees match
        }
        
        if (node1.isLeaf() && node2.isLeaf()) {
            // Found inconsistent segment
            InconsistentSegment segment = new InconsistentSegment();
            segment.setPartitionId(node1.getPartitionId());
            segment.setStartVersion(node1.getStartVersion());
            segment.setEndVersion(node1.getEndVersion());
            segment.setNode1Hash(node1.getHash());
            segment.setNode2Hash(node2.getHash());
            inconsistencies.add(segment);
            return;
        }
        
        // Recurse into children
        if (node1.getLeft() != null && node2.getLeft() != null) {
            compareNodes(node1.getLeft(), node2.getLeft(), inconsistencies);
        }
        if (node1.getRight() != null && node2.getRight() != null) {
            compareNodes(node1.getRight(), node2.getRight(), inconsistencies);
        }
    }
    
    private MerkleNode getOrBuildTree(String partitionId, String nodeUrl) {
        String cacheKey = "merkle:" + partitionId + ":" + extractNodeId(nodeUrl);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            try {
                return deserializeTree(objectMapper.readValue(cached, Map.class));
            } catch (Exception e) {
                // Fall through to rebuild
            }
        }
        
        return buildTree(partitionId, nodeUrl);
    }
    
    private Map<String, Object> serializeTree(MerkleNode node) {
        Map<String, Object> map = new HashMap<>();
        map.put("hash", node.getHash());
        map.put("isLeaf", node.isLeaf());
        if (node.getStartVersion() != null) {
            map.put("startVersion", node.getStartVersion());
            map.put("endVersion", node.getEndVersion());
            map.put("partitionId", node.getPartitionId());
        }
        if (node.getLeft() != null) {
            map.put("left", serializeTree(node.getLeft()));
        }
        if (node.getRight() != null) {
            map.put("right", serializeTree(node.getRight()));
        }
        return map;
    }
    
    private MerkleNode deserializeTree(Map<String, Object> map) {
        String hash = (String) map.get("hash");
        MerkleNode node = new MerkleNode(hash);
        node.setLeaf((Boolean) map.get("isLeaf"));
        
        if (map.containsKey("startVersion")) {
            node.setStartVersion(((Number) map.get("startVersion")).longValue());
            node.setEndVersion(((Number) map.get("endVersion")).longValue());
            node.setPartitionId((String) map.get("partitionId"));
        }
        
        if (map.containsKey("left")) {
            node.setLeft(deserializeTree((Map<String, Object>) map.get("left")));
        }
        if (map.containsKey("right")) {
            node.setRight(deserializeTree((Map<String, Object>) map.get("right")));
        }
        
        return node;
    }
    
    private String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash calculation failed", e);
        }
    }
    
    private String extractNodeId(String url) {
        // Extract node ID from URL (e.g., "http://node1:8081" -> "node1")
        return url.split("//")[1].split(":")[0];
    }
}

class InconsistentSegment {
    private String partitionId;
    private Long startVersion;
    private Long endVersion;
    private String node1Hash;
    private String node2Hash;
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public Long getStartVersion() { return startVersion; }
    public void setStartVersion(Long startVersion) { this.startVersion = startVersion; }
    
    public Long getEndVersion() { return endVersion; }
    public void setEndVersion(Long endVersion) { this.endVersion = endVersion; }
    
    public String getNode1Hash() { return node1Hash; }
    public void setNode1Hash(String node1Hash) { this.node1Hash = node1Hash; }
    
    public String getNode2Hash() { return node2Hash; }
    public void setNode2Hash(String node2Hash) { this.node2Hash = node2Hash; }
}
