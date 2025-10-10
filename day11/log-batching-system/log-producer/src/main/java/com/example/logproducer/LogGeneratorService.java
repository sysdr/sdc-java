package com.example.logproducer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@Service
@Slf4j
public class LogGeneratorService {
    
    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final Counter logsGenerated;
    
    @Value("${log.shipper.url}")
    private String shipperUrl;
    
    private static final String[] LEVELS = {"INFO", "WARN", "ERROR", "DEBUG"};
    private static final String[] SERVICES = {"auth-service", "payment-service", "user-service", "order-service"};
    private static final String[] MESSAGES = {
        "Request processed successfully",
        "Database connection established",
        "Cache miss, fetching from database",
        "User authentication completed",
        "Payment transaction initiated",
        "Order placed successfully",
        "Inventory updated",
        "Email notification sent"
    };
    
    public LogGeneratorService(RestTemplate restTemplate, MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.logsGenerated = Counter.builder("logs.generated")
            .description("Number of logs generated")
            .register(registry);
    }
    
    @Scheduled(fixedRate = 100) // Generate 10 logs per second
    public void generateLogs() {
        try {
            LogEvent event = LogEvent.create(
                LEVELS[random.nextInt(LEVELS.length)],
                SERVICES[random.nextInt(SERVICES.length)],
                MESSAGES[random.nextInt(MESSAGES.length)]
            );
            
            sendToShipper(event);
            logsGenerated.increment();
            
        } catch (Exception e) {
            log.error("Failed to generate log", e);
        }
    }
    
    private void sendToShipper(LogEvent event) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LogEvent> request = new HttpEntity<>(event, headers);
        
        restTemplate.postForEntity(shipperUrl + "/api/logs", request, Void.class);
    }
}
