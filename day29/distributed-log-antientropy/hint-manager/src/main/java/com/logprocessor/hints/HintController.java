package com.logprocessor.hints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hints")
public class HintController {
    
    @Autowired
    private HintService hintService;
    
    @Autowired
    private HintRepository hintRepository;
    
    @PostMapping("/store")
    public ResponseEntity<Map<String, String>> storeHint(@RequestBody HintRequest request) {
        hintService.storeHint(
            request.getTargetNodeUrl(),
            request.getPartitionId(),
            request.getWriteData()
        );
        return ResponseEntity.ok(Map.of("status", "stored"));
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<Hint>> getPendingHints(@RequestParam(required = false) String targetNode) {
        if (targetNode != null) {
            return ResponseEntity.ok(hintRepository.findByTargetNodeUrlAndStatus(targetNode, "PENDING"));
        }
        return ResponseEntity.ok(hintRepository.findByStatusOrderByCreatedAt("PENDING"));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Long pending = hintRepository.countByTargetNodeUrlAndStatus("", "PENDING");
        Long delivered = hintRepository.countByTargetNodeUrlAndStatus("", "DELIVERED");
        Long expired = hintRepository.countByTargetNodeUrlAndStatus("", "EXPIRED");
        
        return ResponseEntity.ok(Map.of(
            "pending", pending,
            "delivered", delivered,
            "expired", expired
        ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

class HintRequest {
    private String targetNodeUrl;
    private String partitionId;
    private Map<String, Object> writeData;
    
    public String getTargetNodeUrl() { return targetNodeUrl; }
    public void setTargetNodeUrl(String targetNodeUrl) { this.targetNodeUrl = targetNodeUrl; }
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public Map<String, Object> getWriteData() { return writeData; }
    public void setWriteData(Map<String, Object> writeData) { this.writeData = writeData; }
}
