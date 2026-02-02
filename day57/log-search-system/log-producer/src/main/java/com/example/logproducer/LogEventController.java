package com.example.logproducer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/logs")
public class LogEventController {
    
    private final KafkaProducerService producerService;
    private static final String[] SERVICES = {"auth-service", "payment-service", "order-service", "inventory-service"};
    private static final String[] SEVERITIES = {"ERROR", "WARN", "INFO", "DEBUG"};
    private static final String[] MESSAGES = {
        "Connection timeout to database",
        "Authentication failed for user",
        "Payment processing completed",
        "Inventory level critical",
        "Cache miss for key",
        "Rate limit exceeded",
        "Request validation failed",
        "External API timeout"
    };
    
    public LogEventController(KafkaProducerService producerService) {
        this.producerService = producerService;
    }
    
    @PostMapping
    public ResponseEntity<Map<String, String>> createLogEvent(@RequestBody LogEvent logEvent) {
        producerService.sendLogEvent(logEvent);
        Map<String, String> response = new HashMap<>();
        response.put("id", logEvent.getId());
        response.put("status", "accepted");
        return ResponseEntity.accepted().body(response);
    }
    
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateRandomLog() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        LogEvent event = LogEvent.createSample(
            SERVICES[random.nextInt(SERVICES.length)],
            SEVERITIES[random.nextInt(SEVERITIES.length)],
            MESSAGES[random.nextInt(MESSAGES.length)]
        );
        
        if ("ERROR".equals(event.getSeverity())) {
            event.setStackTrace("java.lang.RuntimeException: Simulated error\n" +
                "    at com.example.service.SomeService.method(SomeService.java:42)");
        }
        
        producerService.sendLogEvent(event);
        
        Map<String, String> response = new HashMap<>();
        response.put("id", event.getId());
        response.put("status", "generated");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }
}
