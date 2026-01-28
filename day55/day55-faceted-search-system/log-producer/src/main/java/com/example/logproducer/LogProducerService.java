package com.example.logproducer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    private static final String[] LEVELS = {"ERROR", "WARN", "INFO", "DEBUG"};
    private static final String[] SERVICES = {"auth-service", "api-service", "payment-service", "user-service", "order-service"};
    private static final String[] ENVIRONMENTS = {"prod", "staging", "dev"};
    private static final String[] REGIONS = {"us-east-1", "us-west-2", "eu-west-1", "ap-south-1"};
    private static final String[] ERROR_TYPES = {"NullPointerException", "TimeoutException", "ConnectionException", "AuthenticationException", "ValidationException"};
    private static final String[] HOSTS = {"prod-01", "prod-02", "prod-03", "staging-01", "dev-01"};

    // Generate logs with faceted dimensions every 100ms (10 logs/sec)
    @Scheduled(fixedDelay = 100)
    public void generateLogEvent() {
        try {
            LogEvent event = generateRandomLogEvent();
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("logs", event.getId(), json);
            log.debug("Produced log: {}", event.getId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing log event", e);
        }
    }

    private LogEvent generateRandomLogEvent() {
        String level = LEVELS[random.nextInt(LEVELS.length)];
        String service = SERVICES[random.nextInt(SERVICES.length)];
        String environment = ENVIRONMENTS[random.nextInt(ENVIRONMENTS.length)];
        
        // Bias toward ERROR logs for more interesting faceted queries
        if (random.nextDouble() < 0.3) {
            level = "ERROR";
        }

        return LogEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .level(level)
                .service(service)
                .environment(environment)
                .host(HOSTS[random.nextInt(HOSTS.length)])
                .region(REGIONS[random.nextInt(REGIONS.length)])
                .statusCode(generateStatusCode(level))
                .errorType(level.equals("ERROR") ? ERROR_TYPES[random.nextInt(ERROR_TYPES.length)] : null)
                .message(generateMessage(level, service))
                .durationMs(ThreadLocalRandom.current().nextLong(10, 5000))
                .userId("user-" + random.nextInt(1000))
                .traceId(UUID.randomUUID().toString())
                .build();
    }

    private Integer generateStatusCode(String level) {
        if (level.equals("ERROR")) {
            return 500 + random.nextInt(12); // 500-511
        } else if (level.equals("WARN")) {
            return 400 + random.nextInt(18); // 400-417
        }
        return 200;
    }

    private String generateMessage(String level, String service) {
        return switch (level) {
            case "ERROR" -> String.format("%s: Failed to process request", service);
            case "WARN" -> String.format("%s: High latency detected", service);
            case "INFO" -> String.format("%s: Request processed successfully", service);
            default -> String.format("%s: Debug information", service);
        };
    }
}
