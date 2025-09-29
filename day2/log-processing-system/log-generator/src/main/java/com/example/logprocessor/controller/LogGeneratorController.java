package com.example.logprocessor.controller;

import com.example.logprocessor.service.LogGenerationService;
import com.example.logprocessor.service.RateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/generator")
public class LogGeneratorController {
    
    private final LogGenerationService logGenerationService;
    private final RateLimitingService rateLimitingService;
    
    @Autowired
    public LogGeneratorController(LogGenerationService logGenerationService,
                                 RateLimitingService rateLimitingService) {
        this.logGenerationService = logGenerationService;
        this.rateLimitingService = rateLimitingService;
    }
    
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startGeneration() {
        logGenerationService.startGeneration();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "Log generation started successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopGeneration() {
        logGenerationService.stopGeneration();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "stopped");
        response.put("message", "Log generation stopped successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", logGenerationService.isRunning());
        status.put("totalGenerated", logGenerationService.getTotalGenerated());
        status.put("totalRateLimited", logGenerationService.getTotalRateLimited());
        status.put("currentRate", logGenerationService.getCurrentRate());
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/rate/{key}")
    public ResponseEntity<Map<String, Object>> getCurrentRate(@PathVariable String key) {
        long currentRate = rateLimitingService.getCurrentRate(key, 60);
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("currentRate", currentRate);
        response.put("windowSize", 60);
        
        return ResponseEntity.ok(response);
    }
}
