package com.logprocessor.merkle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merkle")
public class MerkleTreeController {
    
    @Autowired
    private MerkleTreeService merkleTreeService;
    
    private final Counter buildCounter;
    private final Counter compareCounter;
    
    public MerkleTreeController(MeterRegistry meterRegistry) {
        this.buildCounter = Counter.builder("merkle_tree_builds_total")
            .description("Total Merkle tree builds")
            .register(meterRegistry);
        this.compareCounter = Counter.builder("merkle_tree_comparisons_total")
            .description("Total Merkle tree comparisons")
            .register(meterRegistry);
    }
    
    @PostMapping("/build")
    public ResponseEntity<Map<String, Object>> buildTree(@RequestBody BuildRequest request) {
        try {
            MerkleNode root = merkleTreeService.buildTree(request.getPartitionId(), request.getNodeUrl());
            buildCounter.increment();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "partitionId", request.getPartitionId(),
                "rootHash", root.getHash(),
                "nodeUrl", request.getNodeUrl()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareTree(@RequestBody CompareRequest request) {
        try {
            List<InconsistentSegment> inconsistencies = merkleTreeService.compareTree(
                request.getPartitionId(),
                request.getNode1Url(),
                request.getNode2Url()
            );
            
            compareCounter.increment();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "partitionId", request.getPartitionId(),
                "inconsistentSegments", inconsistencies.size(),
                "segments", inconsistencies
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

class BuildRequest {
    private String partitionId;
    private String nodeUrl;
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public String getNodeUrl() { return nodeUrl; }
    public void setNodeUrl(String nodeUrl) { this.nodeUrl = nodeUrl; }
}

class CompareRequest {
    private String partitionId;
    private String node1Url;
    private String node2Url;
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public String getNode1Url() { return node1Url; }
    public void setNode1Url(String node1Url) { this.node1Url = node1Url; }
    
    public String getNode2Url() { return node2Url; }
    public void setNode2Url(String node2Url) { this.node2Url = node2Url; }
}
