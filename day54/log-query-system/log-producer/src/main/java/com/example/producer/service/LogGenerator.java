package com.example.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Service
@Slf4j
public class LogGenerator {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    private final String[] LOG_LEVELS = {"INFO", "WARN", "ERROR", "DEBUG"};
    private final String[] SERVICES = {"api-gateway", "user-service", "order-service", "payment-service", "notification-service"};
    
    @Scheduled(fixedRate = 1000)
    public void generateLogs() {
        try {
            for (int i = 0; i < 10; i++) {
                Map<String, Object> logEvent = new HashMap<>();
                logEvent.put("timestamp", System.currentTimeMillis());
                logEvent.put("level", LOG_LEVELS[random.nextInt(LOG_LEVELS.length)]);
                logEvent.put("service", SERVICES[random.nextInt(SERVICES.length)]);
                logEvent.put("message", "Log message " + UUID.randomUUID().toString().substring(0, 8));
                logEvent.put("traceId", UUID.randomUUID().toString());
                logEvent.put("partition", random.nextInt(3));
                
                String message = objectMapper.writeValueAsString(logEvent);
                kafkaTemplate.send("log-events", message);
            }
        } catch (Exception e) {
            log.error("Failed to generate logs", e);
        }
    }
}
