package com.example.logprocessor.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LogConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogConsumerService.class);
    
    private final LogEntryRepository repository;
    private final ObjectMapper objectMapper;
    private final Counter processedCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    
    public LogConsumerService(LogEntryRepository repository,
                             ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        
        this.processedCounter = meterRegistry.counter("kafka.consumer.processed");
        this.errorCounter = meterRegistry.counter("kafka.consumer.error");
        this.processingTimer = meterRegistry.timer("kafka.consumer.processing.duration");
    }
    
    /**
     * Consume and persist log events from Kafka
     */
    @KafkaListener(
        topics = "logs",
        groupId = "log-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeLog(String message, Acknowledgment acknowledgment) {
        processingTimer.record(() -> {
            try {
                // Parse JSON
                JsonNode jsonNode = objectMapper.readTree(message);
                
                // Create entity
                LogEntry entry = new LogEntry();
                entry.setLevel(jsonNode.get("level").asText());
                entry.setTimestamp(Instant.parse(jsonNode.get("timestamp").asText()));
                entry.setMessage(jsonNode.get("message").asText());
                entry.setService(jsonNode.get("service").asText());
                
                // Optional fields
                if (jsonNode.has("metadata")) {
                    entry.setMetadata(jsonNode.get("metadata").toString());
                }
                if (jsonNode.has("traceId")) {
                    entry.setTraceId(jsonNode.get("traceId").asText());
                }
                if (jsonNode.has("spanId")) {
                    entry.setSpanId(jsonNode.get("spanId").asText());
                }
                
                // Persist to PostgreSQL
                repository.save(entry);
                
                // Acknowledge successful processing
                acknowledgment.acknowledge();
                processedCounter.increment();
                
                logger.debug("Log entry persisted: service={}, level={}", 
                           entry.getService(), entry.getLevel());
                
            } catch (Exception e) {
                errorCounter.increment();
                logger.error("Failed to process log message", e);
                // Don't acknowledge - message will be retried
            }
        });
    }
    
    /**
     * Consume from Dead Letter Queue for manual review
     */
    @KafkaListener(
        topics = "logs.dlq",
        groupId = "dlq-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDLQ(String message, Acknowledgment acknowledgment) {
        try {
            logger.warn("DLQ message received: {}", message);
            
            // In production, this would:
            // 1. Store in separate DLQ table for review
            // 2. Alert operations team
            // 3. Attempt automated remediation
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Failed to process DLQ message", e);
        }
    }
}
