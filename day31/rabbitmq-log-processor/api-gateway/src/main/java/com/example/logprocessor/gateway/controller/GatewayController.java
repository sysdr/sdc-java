package com.example.logprocessor.gateway.controller;

import com.example.logprocessor.gateway.service.ProxyService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GatewayController {

    private final ProxyService proxyService;

    @PostMapping("/logs")
    @CircuitBreaker(name = "logProducer", fallbackMethod = "logFallback")
    @RateLimiter(name = "logRateLimiter")
    public ResponseEntity<?> publishLog(@RequestBody Map<String, Object> logEvent) {
        return proxyService.forwardToProducer("/api/v1/logs", logEvent);
    }

    @PostMapping("/logs/batch")
    @CircuitBreaker(name = "logProducer", fallbackMethod = "batchFallback")
    @RateLimiter(name = "batchRateLimiter")
    public ResponseEntity<?> publishBatch(@RequestBody List<Map<String, Object>> logEvents) {
        return proxyService.forwardToProducer("/api/v1/logs/batch", logEvents);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "api-gateway"
        ));
    }

    // Fallback methods
    public ResponseEntity<?> logFallback(Map<String, Object> logEvent, Exception e) {
        return ResponseEntity.status(503).body(Map.of(
            "error", "Service temporarily unavailable",
            "message", "Log producer service is down"
        ));
    }

    public ResponseEntity<?> batchFallback(List<Map<String, Object>> logEvents, Exception e) {
        return ResponseEntity.status(503).body(Map.of(
            "error", "Service temporarily unavailable",
            "message", "Log producer service is down"
        ));
    }
}
