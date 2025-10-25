package com.example.logprocessor.controller;

import com.example.logprocessor.model.LogEvent;
import com.example.logprocessor.service.LogProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    
    private final LogProducerService logProducerService;
    
    public LogController(LogProducerService logProducerService) {
        this.logProducerService = logProducerService;
    }
    
    @PostMapping
    public ResponseEntity<String> sendLog(@RequestBody LogEvent event) {
        logProducerService.sendLog(event);
        return ResponseEntity.ok("Log sent successfully");
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Producer is healthy");
    }
}
