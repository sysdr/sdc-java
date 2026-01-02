package com.example.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class LogGeneratorService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();
    private final AtomicLong counter = new AtomicLong(0);
    
    @Value("${routing.service.url}")
    private String routingServiceUrl;
    
    private static final List<String> SEVERITIES = List.of("DEBUG", "INFO", "WARN", "ERROR", "FATAL");
    private static final List<String> SOURCES = List.of(
        "auth-service", "payment-api", "user-service", "order-service", 
        "notification-service", "analytics-engine"
    );
    private static final List<String> TYPES = List.of("application", "system", "security", "metric", "audit");
    
    @Scheduled(fixedRate = 100) // Generate logs every 100ms
    public void generateLogs() {
        try {
            int batchSize = random.nextInt(10) + 1;
            for (int i = 0; i < batchSize; i++) {
                LogEvent event = createRandomLogEvent();
                sendLog(event);
                counter.incrementAndGet();
            }
            
            if (counter.get() % 1000 == 0) {
                log.info("Generated {} logs", counter.get());
            }
        } catch (Exception e) {
            log.error("Error generating logs", e);
        }
    }
    
    private LogEvent createRandomLogEvent() {
        LogEvent event = new LogEvent();
        event.setSeverity(SEVERITIES.get(random.nextInt(SEVERITIES.size())));
        event.setSource(SOURCES.get(random.nextInt(SOURCES.size())));
        event.setType(TYPES.get(random.nextInt(TYPES.size())));
        event.setMessage(generateMessage(event));
        event.setMetadata(Map.of(
            "userId", "user_" + random.nextInt(1000),
            "requestId", java.util.UUID.randomUUID().toString(),
            "responseTime", random.nextInt(1000)
        ));
        return event;
    }
    
    private String generateMessage(LogEvent event) {
        return switch (event.getType()) {
            case "security" -> "Security event detected from " + event.getSource();
            case "metric" -> "Performance metric: response_time=" + random.nextInt(1000) + "ms";
            case "audit" -> "User action logged: " + event.getSeverity();
            default -> "Application log from " + event.getSource();
        };
    }
    
    private void sendLog(LogEvent event) {
        try {
            restTemplate.postForEntity(
                routingServiceUrl + "/api/logs",
                event,
                String.class
            );
        } catch (Exception e) {
            log.error("Failed to send log", e);
        }
    }
}
