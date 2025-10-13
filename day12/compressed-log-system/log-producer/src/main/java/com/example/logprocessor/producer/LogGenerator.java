package com.example.logprocessor.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Component
public class LogGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LogGenerator.class);
    private final KafkaProducerService producerService;
    private final Random random = new Random();

    private static final String[] SERVICES = {"auth", "payment", "order", "inventory", "notification"};
    private static final String[] LEVELS = {"INFO", "WARN", "ERROR", "DEBUG"};
    private static final String[] MESSAGES = {
        "User authentication successful",
        "Payment processed for order",
        "Inventory updated for product",
        "Notification sent to user",
        "Cache invalidated for key",
        "Database connection pool exhausted",
        "API rate limit exceeded",
        "Background job completed"
    };

    public LogGenerator(KafkaProducerService producerService) {
        this.producerService = producerService;
    }

    @Scheduled(fixedRate = 1000)
    public void generateLogs() {
        int count = random.nextInt(5) + 1;
        for (int i = 0; i < count; i++) {
            String log = createRandomLog();
            producerService.sendLog(log);
        }
    }

    private String createRandomLog() {
        return String.format(
            "{\"timestamp\":\"%s\",\"level\":\"%s\",\"service\":\"%s\",\"message\":\"%s\"," +
            "\"metadata\":{\"user_id\":\"%s\",\"request_id\":\"%s\",\"duration_ms\":%d}}",
            Instant.now(),
            LEVELS[random.nextInt(LEVELS.length)],
            SERVICES[random.nextInt(SERVICES.length)],
            MESSAGES[random.nextInt(MESSAGES.length)],
            "user_" + random.nextInt(1000),
            UUID.randomUUID().toString(),
            random.nextInt(1000)
        );
    }
}
