package com.example.logindexing.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class LogProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter logsProduced;
    private final Counter logsProducedErrors;
    private final Random random = new Random();

    private static final String TOPIC = "raw-logs";
    private static final String[] LEVELS = {"DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
    private static final String[] SERVICES = {"auth-service", "user-service", "payment-service", "notification-service"};
    private static final String[] MESSAGES = {
        "User authentication successful",
        "Failed to connect to database",
        "Request processing completed",
        "Invalid input parameters",
        "Service unavailable",
        "Connection timeout",
        "Transaction completed successfully",
        "Cache miss, fetching from database",
        "Rate limit exceeded",
        "Payment processed successfully"
    };

    public LogProducerService(KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logsProduced = Counter.builder("logs.produced.total")
                .description("Total number of logs produced")
                .register(meterRegistry);
        this.logsProducedErrors = Counter.builder("logs.produced.errors")
                .description("Total number of log production errors")
                .register(meterRegistry);
    }

    public CompletableFuture<SendResult<String, String>> produceLog(LogEvent logEvent) {
        try {
            String logJson = objectMapper.writeValueAsString(logEvent);
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(TOPIC, logEvent.getId(), logJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logsProduced.increment();
                    log.debug("Produced log: {} to partition {}", 
                        logEvent.getId(), result.getRecordMetadata().partition());
                } else {
                    logsProducedErrors.increment();
                    log.error("Failed to produce log: {}", logEvent.getId(), ex);
                }
            });
            
            return future;
        } catch (JsonProcessingException e) {
            logsProducedErrors.increment();
            log.error("Failed to serialize log event", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public LogEvent generateRandomLog() {
        return LogEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .level(LEVELS[random.nextInt(LEVELS.length)])
                .service(SERVICES[random.nextInt(SERVICES.length)])
                .message(MESSAGES[random.nextInt(MESSAGES.length)])
                .userId("user-" + random.nextInt(1000))
                .traceId(UUID.randomUUID().toString())
                .metadata(Map.of(
                    "region", "us-west-" + (random.nextInt(3) + 1),
                    "environment", random.nextBoolean() ? "production" : "staging"
                ))
                .build();
    }
}
