package com.example.gateway;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dlq")
public class DLQController {
    
    private final DLQManagementService dlqService;
    
    public DLQController(DLQManagementService dlqService) {
        this.dlqService = dlqService;
    }
    
    @GetMapping("/messages")
    public ResponseEntity<List<DLQMessage>> getMessages(
            @RequestParam(required = false) String errorType,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<DLQMessage> messages = dlqService.getDLQMessages(errorType, offset, limit);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<DLQMessage> getMessage(@PathVariable String messageId) {
        return dlqService.getDLQMessage(messageId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/reprocess/{messageId}")
    public ResponseEntity<Map<String, String>> reprocessMessage(@PathVariable String messageId) {
        boolean success = dlqService.reprocessMessage(messageId);
        if (success) {
            return ResponseEntity.ok(Map.of("status", "reprocessed", "messageId", messageId));
        }
        return ResponseEntity.badRequest()
            .body(Map.of("status", "failed", "message", "Message not found"));
    }
    
    @PostMapping("/reprocess/batch")
    public ResponseEntity<Map<String, Object>> reprocessBatch(
            @RequestParam(required = false) String errorType,
            @RequestParam(defaultValue = "100") int limit) {
        
        int reprocessed = dlqService.reprocessBatch(errorType, limit);
        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "reprocessed", reprocessed
        ));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(dlqService.getDLQStats());
    }
}
