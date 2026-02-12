package com.example.logprocessor.service;

import com.example.logprocessor.model.LogEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "log-events";
    
    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @CircuitBreaker(name = "kafka-producer", fallbackMethod = "sendToDeadLetter")
    public CompletableFuture<SendResult<String, LogEvent>> sendLog(LogEvent event) {
        logger.debug("Sending log event: {}", event.getId());
        
        CompletableFuture<SendResult<String, LogEvent>> future = kafkaTemplate.send(TOPIC, event.getId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.debug("Log sent successfully: partition={}, offset={}", 
                    result.getRecordMetadata().partition(), 
                    result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send log event: {}", event.getId(), ex);
            }
        });
        
        return future;
    }

    /**
     * Fallback method when circuit breaker is open
     * In production, this would write to a dead letter queue or local disk
     */
    public CompletableFuture<SendResult<String, LogEvent>> sendToDeadLetter(LogEvent event, Exception ex) {
        logger.warn("Circuit breaker OPEN - writing to dead letter queue: {}", event.getId());
        
        // Simulate writing to DLQ
        CompletableFuture<SendResult<String, LogEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Service degraded: " + ex.getMessage()));
        
        return future;
    }
}
