package com.example.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LogGenerator {
    private static final Logger logger = LoggerFactory.getLogger(LogGenerator.class);
    
    private static final String[] SERVICES = {
        "api-gateway", "auth-service", "user-service", "order-service",
        "payment-service", "notification-service", "inventory-service", "analytics-service"
    };
    
    private static final String[] LOG_LEVELS = {"INFO", "WARN", "ERROR"};
    private static final int[] LEVEL_WEIGHTS = {70, 20, 10}; // 70% INFO, 20% WARN, 10% ERROR
    
    private final KafkaProducerService producerService;
    private long eventCounter = 0;
    
    public LogGenerator(KafkaProducerService producerService) {
        this.producerService = producerService;
    }
    
    @Scheduled(fixedRate = 20) // Generate events every 20ms = 50 events/sec per service
    public void generateLogs() {
        for (String service : SERVICES) {
            LogEvent event = generateLogEvent(service);
            producerService.sendLogEvent(event);
        }
    }
    
    private LogEvent generateLogEvent(String service) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Weighted random level selection
        int weight = random.nextInt(100);
        String level;
        if (weight < LEVEL_WEIGHTS[0]) {
            level = "INFO";
        } else if (weight < LEVEL_WEIGHTS[0] + LEVEL_WEIGHTS[1]) {
            level = "WARN";
        } else {
            level = "ERROR";
        }
        
        // Generate realistic latencies and status codes based on level
        int latency;
        int statusCode;
        
        if (level.equals("ERROR")) {
            latency = random.nextInt(500, 5000); // 500-5000ms for errors
            statusCode = 500 + random.nextInt(10); // 500-509
        } else if (level.equals("WARN")) {
            latency = random.nextInt(200, 1000); // 200-1000ms for warnings
            statusCode = random.nextBoolean() ? 200 : (400 + random.nextInt(10)); // 200 or 400-409
        } else {
            latency = random.nextInt(10, 200); // 10-200ms for info
            statusCode = 200;
        }
        
        // Add some late-arriving events (5% arrive 1-5 seconds late)
        long eventTime = Instant.now().toEpochMilli();
        if (random.nextInt(100) < 5) {
            eventTime -= random.nextInt(1000, 5000);
        }
        
        return LogEvent.builder()
            .eventId(String.format("%s-%d", service, eventCounter++))
            .timestamp(eventTime)
            .service(service)
            .level(level)
            .message(generateMessage(level))
            .latencyMs(latency)
            .statusCode(statusCode)
            .attributes(generateAttributes(service))
            .build();
    }
    
    private String generateMessage(String level) {
        return switch (level) {
            case "ERROR" -> "Request failed: " + getRandomError();
            case "WARN" -> "Performance degradation: " + getRandomWarning();
            default -> "Request processed successfully";
        };
    }
    
    private String getRandomError() {
        String[] errors = {
            "Database connection timeout",
            "Service unavailable",
            "Invalid request parameters",
            "Authentication failed",
            "Rate limit exceeded"
        };
        return errors[ThreadLocalRandom.current().nextInt(errors.length)];
    }
    
    private String getRandomWarning() {
        String[] warnings = {
            "High latency detected",
            "Cache miss rate elevated",
            "Queue depth increasing",
            "Memory usage high",
            "Retry attempt"
        };
        return warnings[ThreadLocalRandom.current().nextInt(warnings.length)];
    }
    
    private Map<String, String> generateAttributes(String service) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("env", "production");
        attrs.put("region", getRandomRegion());
        attrs.put("version", "1.0.0");
        attrs.put("host", service + "-" + ThreadLocalRandom.current().nextInt(1, 11));
        return attrs;
    }
    
    private String getRandomRegion() {
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1"};
        return regions[ThreadLocalRandom.current().nextInt(regions.length)];
    }
}
