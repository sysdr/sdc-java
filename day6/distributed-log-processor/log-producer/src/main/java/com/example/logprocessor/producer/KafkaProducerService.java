package com.example.logprocessor.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC_NAME = "log-events";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @CircuitBreaker(name = "kafka-producer", fallbackMethod = "fallbackSendLogEvent")
    @Retry(name = "kafka-producer")
    public CompletableFuture<SendResult<String, String>> sendLogEvent(LogEvent logEvent) {
        try {
            String messageJson = objectMapper.writeValueAsString(logEvent);
            String key = logEvent.source() + "-" + logEvent.level();
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC_NAME, key, messageJson);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.debug("Sent log event with key=[{}] to partition=[{}] with offset=[{}]",
                            key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send log event with key=[{}]", key, exception);
                }
            });
            
            return future;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize log event", e);
            throw new RuntimeException("Failed to serialize log event", e);
        }
    }

    @CircuitBreaker(name = "kafka-producer", fallbackMethod = "fallbackSendLogEventsBatch")
    @Retry(name = "kafka-producer")
    public CompletableFuture<Void> sendLogEventsBatch(List<LogEvent> logEvents) {
        List<CompletableFuture<SendResult<String, String>>> futures = logEvents.stream()
                .map(this::sendLogEvent)
                .toList();
                
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // Circuit breaker fallback methods
    public CompletableFuture<SendResult<String, String>> fallbackSendLogEvent(LogEvent logEvent, Exception ex) {
        logger.error("Circuit breaker activated for single log event. Storing to local queue.", ex);
        // In production, implement local queue storage
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable"));
    }

    public CompletableFuture<Void> fallbackSendLogEventsBatch(List<LogEvent> logEvents, Exception ex) {
        logger.error("Circuit breaker activated for batch log events. Storing to local queue.", ex);
        // In production, implement local queue storage
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable"));
    }
}
