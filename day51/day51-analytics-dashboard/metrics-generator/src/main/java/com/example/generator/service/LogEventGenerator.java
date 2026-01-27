package com.example.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates synthetic log events for dashboard testing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogEventGenerator {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    private final String[] services = {"api-gateway", "user-service", "order-service", "payment-service"};
    private final String[] endpoints = {"/api/users", "/api/orders", "/api/payments", "/api/products"};
    private final String[] levels = {"INFO", "WARN", "ERROR"};
    
    @Scheduled(fixedRate = 100)
    public void generateLogEvents() {
        for (int i = 0; i < 10; i++) {
            Map<String, Object> event = new HashMap<>();
            event.put("timestamp", Instant.now().toString());
            event.put("level", getRandomLevel());
            event.put("service", services[random.nextInt(services.length)]);
            event.put("message", "Processing request");
            event.put("responseTime", 50 + random.nextInt(450));
            event.put("endpoint", endpoints[random.nextInt(endpoints.length)]);
            event.put("statusCode", getRandomStatusCode());
            
            try {
                String message = objectMapper.writeValueAsString(event);
                kafkaTemplate.send("log-events", message);
            } catch (Exception e) {
                log.error("Error generating event: {}", e.getMessage());
            }
        }
    }
    
    private String getRandomLevel() {
        int rand = random.nextInt(100);
        if (rand < 80) return "INFO";
        if (rand < 95) return "WARN";
        return "ERROR";
    }
    
    private int getRandomStatusCode() {
        int rand = random.nextInt(100);
        if (rand < 90) return 200;
        if (rand < 95) return 400;
        return 500;
    }
}
