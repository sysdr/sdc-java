package com.example.logprocessor.gateway;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> response = Map.of(
            "service", "API Gateway",
            "status", "running",
            "version", "1.0.0",
            "description", "Distributed Log Processing System - API Gateway",
            "endpoints", "/api/v1/logs, /actuator/health, /h2-console"
        );
        return ResponseEntity.ok(response);
    }
}
