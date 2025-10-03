package com.example.logprocessor.consumer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> response = Map.of(
            "service", "Log Consumer",
            "status", "running",
            "version", "1.0.0",
            "description", "Distributed Log Processing System - Log Consumer Service",
            "endpoints", "/actuator/health"
        );
        return ResponseEntity.ok(response);
    }
}
