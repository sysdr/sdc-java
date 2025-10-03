package com.example.logprocessor.producer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> response = Map.of(
            "service", "Log Producer",
            "status", "running",
            "version", "1.0.0",
            "description", "Distributed Log Processing System - Log Producer Service",
            "endpoints", "/api/v1/logs, /actuator/health"
        );
        return ResponseEntity.ok(response);
    }
}
