package com.example.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();
    
    private static final String[] SERVICES = {"checkout", "payment", "inventory", "shipping"};
    private static final String[] LEVELS = {"INFO", "WARN", "ERROR"};
    private static final String[] MESSAGES = {
            "Request processed successfully",
            "Database connection timeout",
            "Payment validation failed",
            "Inventory check completed",
            "Order shipped successfully"
    };

    @Scheduled(fixedRate = 1000) // Generate logs every second
    public void generateLogs() {
        for (int i = 0; i < 10; i++) {
            String service = SERVICES[random.nextInt(SERVICES.length)];
            String level = LEVELS[random.nextInt(LEVELS.length)];
            String message = MESSAGES[random.nextInt(MESSAGES.length)];
            
            String logEntry = String.format(
                    "{\"timestamp\":\"%s\",\"service\":\"%s\",\"level\":\"%s\"," +
                    "\"message\":\"%s\",\"traceId\":\"%s\",\"hostname\":\"producer-1\"}",
                    Instant.now().toString(),
                    service,
                    level,
                    message,
                    UUID.randomUUID().toString()
            );
            
            kafkaTemplate.send("logs", service, logEntry);
        }
    }
}
