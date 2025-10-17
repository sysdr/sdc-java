package com.example.logprocessor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "log-events";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter successCounter;
    private final Counter failureCounter;

    public KafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.successCounter = Counter.builder("log.producer.success")
            .description("Successful log events sent")
            .register(meterRegistry);
        this.failureCounter = Counter.builder("log.producer.failure")
            .description("Failed log events")
            .register(meterRegistry);
    }

    @CircuitBreaker(name = "kafka-producer", fallbackMethod = "fallbackSendLog")
    public CompletableFuture<SendResult<String, String>> sendLog(LogEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(TOPIC, event.id(), json);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    successCounter.increment();
                    logger.debug("Sent log event: {} to partition: {}", 
                        event.id(), result.getRecordMetadata().partition());
                } else {
                    failureCounter.increment();
                    logger.error("Failed to send log event: {}", event.id(), ex);
                }
            });
            
            return future;
        } catch (Exception e) {
            failureCounter.increment();
            logger.error("Error serializing log event: {}", event.id(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<SendResult<String, String>> fallbackSendLog(
            LogEvent event, Exception ex) {
        logger.warn("Circuit breaker activated for log event: {}", event.id());
        failureCounter.increment();
        return CompletableFuture.failedFuture(
            new RuntimeException("Service temporarily unavailable", ex));
    }
}
