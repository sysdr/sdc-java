package com.example.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {

    @Value("${producer.url:http://localhost:8081}")
    private String producerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/logs")
    public ResponseEntity<?> forwardLog(@RequestBody Map<String, Object> logData) {
        String url = producerUrl + "/api/logs";
        return restTemplate.postForEntity(url, logData, String.class);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "api-gateway"));
    }
}
