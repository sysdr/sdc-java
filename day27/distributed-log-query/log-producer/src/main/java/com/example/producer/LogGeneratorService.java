package com.example.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogGeneratorService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String[] SERVICES = {
        "payment-service", "user-service", "order-service", 
        "inventory-service", "notification-service"
    };
    
    private static final String[] LOG_LEVELS = {"INFO", "WARN", "ERROR", "DEBUG"};
    private static final String[] MESSAGES = {
        "Request processed successfully",
        "Database query executed",
        "Cache miss occurred",
        "API call failed with timeout",
        "User authentication successful",
        "Payment transaction completed",
        "Order validation failed",
        "Inventory updated",
        "Notification sent"
    };
    
    private final Random random = new Random();
    
    @Scheduled(fixedDelay = 1000) // Generate logs every second
    public void generateLogs() {
        for (int i = 0; i < 10; i++) {
            try {
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("timestamp", Instant.now().toString());
                logEntry.put("level", LOG_LEVELS[random.nextInt(LOG_LEVELS.length)]);
                logEntry.put("serviceName", SERVICES[random.nextInt(SERVICES.length)]);
                logEntry.put("message", MESSAGES[random.nextInt(MESSAGES.length)]);
                logEntry.put("traceId", UUID.randomUUID().toString());
                logEntry.put("partitionId", "partition-" + (random.nextInt(3) + 1));
                
                String json = objectMapper.writeValueAsString(logEntry);
                kafkaTemplate.send("logs", json);
                
            } catch (Exception e) {
                log.error("Error generating log", e);
            }
        }
    }
}
