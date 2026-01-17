package com.example.logproducer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LogEventGenerator {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final Random random = new Random();
    private final AtomicLong eventCounter = new AtomicLong(0);

    private final String[] ENDPOINTS = {
        "/api/users", "/api/orders", "/api/products", "/api/payments",
        "/api/inventory", "/api/search", "/api/recommendations", "/api/cart"
    };

    private final String[] METHODS = {"GET", "POST", "PUT", "DELETE"};
    private final String[] REGIONS = {"us-east-1", "us-west-2", "eu-west-1", "ap-south-1"};
    private final String[] SERVICES = {"web-app", "mobile-app", "api-gateway", "background-worker"};

    public LogEventGenerator(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 25) // ~40 events per second per instance
    public void generateLogEvents() {
        for (int i = 0; i < 10; i++) {
            LogEvent event = createRandomLogEvent();
            kafkaTemplate.send("log-events", event.getEndpoint(), event);
            eventCounter.incrementAndGet();
        }
    }

    private LogEvent createRandomLogEvent() {
        String endpoint = ENDPOINTS[random.nextInt(ENDPOINTS.length)];
        int statusCode = generateWeightedStatusCode();
        long responseTime = generateResponseTime(statusCode);

        return LogEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .endpoint(endpoint)
            .method(METHODS[random.nextInt(METHODS.length)])
            .statusCode(statusCode)
            .responseTimeMs(responseTime)
            .userId("user-" + random.nextInt(10000))
            .region(REGIONS[random.nextInt(REGIONS.length)])
            .service(SERVICES[random.nextInt(SERVICES.length)])
            .build();
    }

    private int generateWeightedStatusCode() {
        int rand = random.nextInt(100);
        if (rand < 80) return 200; // 80% success
        if (rand < 90) return 201; // 10% created
        if (rand < 95) return 400; // 5% client error
        if (rand < 98) return 404; // 3% not found
        return 500; // 2% server error
    }

    private long generateResponseTime(int statusCode) {
        if (statusCode >= 500) {
            return 1000 + random.nextInt(4000); // 1-5 seconds for errors
        } else if (statusCode >= 400) {
            return 100 + random.nextInt(400); // 100-500ms for client errors
        } else {
            // Normal distribution around 150ms for success
            return (long) Math.max(10, random.nextGaussian() * 100 + 150);
        }
    }

    public long getEventCount() {
        return eventCounter.get();
    }
}
