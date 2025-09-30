package com.example.logprocessor.generator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GeneratorController {
    
    @Autowired
    private LogGeneratorService logGeneratorService;

    @GetMapping("/")
    public Map<String, Object> welcome() {
        return Map.of(
            "service", "Log Generator Service",
            "status", "running",
            "endpoints", Map.of(
                "stats", "/api/generator/stats",
                "health", "/actuator/health"
            )
        );
    }

    @GetMapping("/api/generator/stats")
    public Map<String, Object> getStats() {
        return Map.of(
            "totalEventsGenerated", logGeneratorService.getGeneratedEventCount(),
            "status", "running"
        );
    }
}
