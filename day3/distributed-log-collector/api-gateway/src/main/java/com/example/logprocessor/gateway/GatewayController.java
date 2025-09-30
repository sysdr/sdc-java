package com.example.logprocessor.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class GatewayController {
    
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/")
    public Map<String, Object> welcome() {
        return Map.of(
            "service", "API Gateway",
            "status", "running",
            "endpoints", Map.of(
                "health", "/api/health",
                "system-stats", "/api/system/stats",
                "generator-stats", "http://localhost:8081/api/generator/stats",
                "collector-stats", "http://localhost:8082/api/collector/stats"
            )
        );
    }

    @GetMapping("/api/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
            "status", "healthy",
            "timestamp", System.currentTimeMillis(),
            "service", "api-gateway"
        );
    }

    @GetMapping("/api/system/stats")
    public Map<String, Object> getSystemStats() {
        try {
            Map<String, Object> generatorStats = restTemplate.getForObject(
                "http://localhost:8081/api/generator/stats", Map.class);
            Map<String, Object> collectorStats = restTemplate.getForObject(
                "http://localhost:8082/api/collector/stats", Map.class);
            
            return Map.of(
                "generator", generatorStats != null ? generatorStats : Map.of("status", "unavailable"),
                "collector", collectorStats != null ? collectorStats : Map.of("status", "unavailable"),
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            return Map.of(
                "error", "Failed to fetch system stats",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
}
