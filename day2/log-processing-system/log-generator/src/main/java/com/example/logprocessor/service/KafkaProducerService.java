package com.example.logprocessor.service;

import com.example.logprocessor.model.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter sentCounter;
    private final Counter failedCounter;
    private final Timer sendTimer;
    
    @Value("${app.kafka.topic:log-events}")
    private String topicName;
    
    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, 
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.sentCounter = Counter.builder("kafka_messages_sent")
                .description("Number of messages sent to Kafka")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("kafka_messages_failed")
                .description("Number of failed Kafka sends")
                .register(meterRegistry);
        this.sendTimer = Timer.builder("kafka_send_duration")
                .description("Time taken to send messages to Kafka")
                .register(meterRegistry);
    }
    
    public CompletableFuture<Void> sendLogEvent(LogEvent logEvent) {
        try {
            return sendTimer.recordCallable(() -> {
            try {
                String eventJson = objectMapper.writeValueAsString(logEvent);
                CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(topicName, logEvent.getCorrelationId(), eventJson).completable();
                
                return future.handle((result, throwable) -> {
                    if (throwable != null) {
                        failedCounter.increment();
                        logger.error("Failed to send log event: {}", throwable.getMessage());
                        throw new RuntimeException(throwable);
                    } else {
                        sentCounter.increment();
                        logger.debug("Sent log event with correlation ID: {}", logEvent.getCorrelationId());
                        return null;
                    }
                });
            } catch (Exception e) {
                failedCounter.increment();
                throw new RuntimeException("Failed to serialize log event", e);
            }
        });
        } catch (Exception e) {
            failedCounter.increment();
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }
    
    /**
     * Batch send for improved throughput
     */
    public CompletableFuture<Void> sendLogEventsBatch(List<LogEvent> logEvents) {
        Timer.Sample sample = Timer.start();
        
        List<CompletableFuture<SendResult<String, String>>> futures = logEvents.stream()
            .map(event -> {
                try {
                    String eventJson = objectMapper.writeValueAsString(event);
                    return kafkaTemplate.send(topicName, event.getCorrelationId(), eventJson).completable();
                } catch (Exception e) {
                    failedCounter.increment();
                    CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(e);
                    return failedFuture;
                }
            })
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .handle((result, throwable) -> {
                sample.stop(sendTimer);
                if (throwable != null) {
                    logger.error("Batch send failed: {}", throwable.getMessage());
                    throw new RuntimeException(throwable);
                } else {
                    sentCounter.increment(logEvents.size());
                    logger.debug("Sent batch of {} log events", logEvents.size());
                    return null;
                }
            });
    }
    
    // Circuit breaker fallback methods
    public CompletableFuture<Void> fallbackSend(LogEvent logEvent, Exception ex) {
        logger.warn("Circuit breaker fallback for single event: {}", ex.getMessage());
        return CompletableFuture.completedFuture(null);
    }
    
    public CompletableFuture<Void> fallbackSendBatch(List<LogEvent> logEvents, Exception ex) {
        logger.warn("Circuit breaker fallback for batch of {} events: {}", logEvents.size(), ex.getMessage());
        return CompletableFuture.completedFuture(null);
    }
}
