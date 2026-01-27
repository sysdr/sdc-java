package com.example.logproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LogEventGenerator {
    private static final Logger logger = LoggerFactory.getLogger(LogEventGenerator.class);
    private final KafkaProducerService kafkaProducerService;
    private final Random random = new Random();
    
    private static final String[] SERVICES = {"auth-service", "payment-service", "order-service", "inventory-service"};
    private static final String[] ENDPOINTS = {"/api/login", "/api/checkout", "/api/orders", "/api/inventory"};
    
    public LogEventGenerator(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }
    
    @Scheduled(fixedRate = 100) // Generate 10 events per second
    public void generateNormalEvents() {
        for (int i = 0; i < 10; i++) {
            LogEvent event = createNormalEvent();
            kafkaProducerService.sendLogEvent(event);
        }
    }
    
    @Scheduled(fixedRate = 5000) // Inject anomalies every 5 seconds
    public void generateAnomalies() {
        // Inject different types of anomalies
        if (random.nextDouble() < 0.3) {
            LogEvent event = createHighLatencyAnomaly();
            kafkaProducerService.sendLogEvent(event);
            logger.info("Injected high latency anomaly: {} ms", event.getResponseTime());
        }
        
        if (random.nextDouble() < 0.2) {
            LogEvent event = createHighCpuAnomaly();
            kafkaProducerService.sendLogEvent(event);
            logger.info("Injected high CPU anomaly: {}%", event.getCpuUsage());
        }
        
        if (random.nextDouble() < 0.2) {
            LogEvent event = createErrorBurstAnomaly();
            kafkaProducerService.sendLogEvent(event);
            logger.info("Injected error burst anomaly: {} errors", event.getErrorCount());
        }
    }
    
    private LogEvent createNormalEvent() {
        String eventId = UUID.randomUUID().toString();
        String userId = "user-" + ThreadLocalRandom.current().nextInt(1000);
        String service = SERVICES[random.nextInt(SERVICES.length)];
        String endpoint = ENDPOINTS[random.nextInt(ENDPOINTS.length)];
        
        // Normal distributions
        int responseTime = (int) (100 + random.nextGaussian() * 20); // Mean 100ms, stddev 20ms
        int statusCode = random.nextDouble() < 0.95 ? 200 : (random.nextDouble() < 0.5 ? 404 : 500);
        double cpuUsage = 30 + random.nextGaussian() * 5; // Mean 30%, stddev 5%
        double memoryUsage = 50 + random.nextGaussian() * 8; // Mean 50%, stddev 8%
        int errorCount = random.nextDouble() < 0.9 ? 0 : random.nextInt(3);
        
        return new LogEvent(eventId, userId, service, endpoint, 
                          Math.max(1, responseTime), statusCode,
                          Math.max(0, cpuUsage), Math.max(0, memoryUsage), 
                          errorCount);
    }
    
    private LogEvent createHighLatencyAnomaly() {
        LogEvent event = createNormalEvent();
        event.setResponseTime(500 + random.nextInt(1500)); // 500-2000ms
        event.setAnomaly(true);
        return event;
    }
    
    private LogEvent createHighCpuAnomaly() {
        LogEvent event = createNormalEvent();
        event.setCpuUsage(85 + random.nextDouble() * 15); // 85-100%
        event.setAnomaly(true);
        return event;
    }
    
    private LogEvent createErrorBurstAnomaly() {
        LogEvent event = createNormalEvent();
        event.setErrorCount(10 + random.nextInt(20)); // 10-30 errors
        event.setStatusCode(500);
        event.setAnomaly(true);
        return event;
    }
}
