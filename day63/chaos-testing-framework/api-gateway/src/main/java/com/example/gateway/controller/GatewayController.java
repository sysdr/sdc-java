package com.example.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String PRODUCER_URL = "http://localhost:8081/api/logs";

    @PostMapping("/logs")
    public ResponseEntity<?> forwardLog(@RequestBody Map<String, Object> logData) {
        return restTemplate.postForEntity(PRODUCER_URL, logData, Map.class);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "api-gateway"));
    }
}
