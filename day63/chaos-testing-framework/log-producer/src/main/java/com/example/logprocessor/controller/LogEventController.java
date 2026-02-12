package com.example.logprocessor.controller;

import com.example.logprocessor.model.LogEvent;
import com.example.logprocessor.service.KafkaProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/logs")
public class LogEventController {
    
    private final KafkaProducerService producerService;
    private final Counter successCounter;
    private final Counter failureCounter;

    public LogEventController(KafkaProducerService producerService, MeterRegistry meterRegistry) {
        this.producerService = producerService;
        this.successCounter = meterRegistry.counter("log.events.sent.success");
        this.failureCounter = meterRegistry.counter("log.events.sent.failure");
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> ingestLog(@RequestBody LogEvent event) {
        try {
            CompletableFuture<?>  future = producerService.sendLog(event);
            
            // Wait with timeout for circuit breaker validation
            future.get(6, TimeUnit.SECONDS);
            
            successCounter.increment();
            
            Map<String, String> response = new HashMap<>();
            response.put("id", event.getId());
            response.put("status", "accepted");
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            failureCounter.increment();
            
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "failed");
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "log-producer");
        return ResponseEntity.ok(health);
    }
}
