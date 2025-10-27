package com.example.logprocessor.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC_LOGS = "logs";
    private static final String TOPIC_DLQ = "logs.dlq";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter dlqCounter;
    private final Timer sendTimer;
    
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        
        // Initialize metrics
        this.successCounter = meterRegistry.counter("kafka.producer.success");
        this.failureCounter = meterRegistry.counter("kafka.producer.failure");
        this.dlqCounter = meterRegistry.counter("kafka.producer.dlq");
        this.sendTimer = meterRegistry.timer("kafka.producer.send.duration");
    }
    
    /**
     * Send valid log event to Kafka
     */
    public CompletableFuture<SendResult<String, String>> sendLog(LogEvent event) {
        return sendTimer.record(() -> {
            try {
                String json = objectMapper.writeValueAsString(event);
                String key = event.getService(); // Partition by service for ordering
                
                CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(TOPIC_LOGS, key, json);
                
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        successCounter.increment();
                        logger.debug("Log sent successfully: partition={}, offset={}", 
                                   result.getRecordMetadata().partition(),
                                   result.getRecordMetadata().offset());
                    } else {
                        failureCounter.increment();
                        logger.error("Failed to send log to Kafka", ex);
                    }
                });
                
                return future;
                
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize log event", e);
                CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(e);
                return failedFuture;
            }
        });
    }
    
    /**
     * Send invalid log event to Dead Letter Queue
     */
    public void sendToDLQ(LogEvent event, String validationError) {
        try {
            // Add error metadata to event
            if (event.getMetadata() == null) {
                event.setMetadata(new java.util.HashMap<>());
            }
            event.getMetadata().put("validation_error", validationError);
            event.getMetadata().put("dlq_timestamp", java.time.Instant.now().toString());
            
            String json = objectMapper.writeValueAsString(event);
            String key = event.getService();
            
            kafkaTemplate.send(TOPIC_DLQ, key, json)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        dlqCounter.increment();
                        logger.warn("Invalid log sent to DLQ: {}", validationError);
                    } else {
                        logger.error("Failed to send to DLQ", ex);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Critical: Failed to send to DLQ", e);
        }
    }
}
