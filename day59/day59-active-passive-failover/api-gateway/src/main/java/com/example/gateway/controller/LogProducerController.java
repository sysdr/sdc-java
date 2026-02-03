package com.example.gateway.controller;

import com.example.gateway.model.LogEventRequest;
import com.example.gateway.service.LogProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@Slf4j
public class LogProducerController {
    
    private final LogProducerService producerService;
    
    public LogProducerController(LogProducerService producerService) {
        this.producerService = producerService;
    }
    
    @PostMapping
    public ResponseEntity<Map<String, String>> publishLog(@RequestBody LogEventRequest request) {
        try {
            String messageId = producerService.publishLogEvent(request);
            return ResponseEntity.ok(Map.of(
                "status", "published",
                "messageId", messageId
            ));
        } catch (Exception e) {
            log.error("Failed to publish log", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "api-gateway"
        ));
    }
}
