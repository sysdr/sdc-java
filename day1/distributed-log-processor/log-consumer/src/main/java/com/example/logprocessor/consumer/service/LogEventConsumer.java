package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEvent;
import com.example.logprocessor.consumer.repository.LogEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(LogEventConsumer.class);
    
    @Autowired
    private LogEventRepository logEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final Counter processedCounter;
    private final Counter errorCounter;
    
    public LogEventConsumer(MeterRegistry meterRegistry) {
        this.processedCounter = Counter.builder("log_events_processed_total")
                .description("Total number of log events processed")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("log_events_errors_total")
                .description("Total number of log event processing errors")
                .register(meterRegistry);
    }
    
    @KafkaListener(topics = "log-events", groupId = "log-consumer-group")
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        autoCreateTopics = "true",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    public void consume(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            Acknowledgment acknowledgment) {
        
        try {
            logger.debug("Received message from topic: {}, partition: {}", topic, partition);
            
            LogEvent logEvent = objectMapper.readValue(message, LogEvent.class);
            processLogEvent(logEvent);
            
            acknowledgment.acknowledge();
            processedCounter.increment();
            
            logger.debug("Successfully processed log event: {}", logEvent.getId());
            
        } catch (Exception e) {
            logger.error("Failed to process message: {}", message, e);
            errorCounter.increment();
            throw new RuntimeException("Failed to process log event", e);
        }
    }
    
    @Retryable(value = {DataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processLogEvent(LogEvent logEvent) {
        try {
            // Set processing timestamp
            logEvent.setProcessedAt(LocalDateTime.now());
            
            // Save to database
            logEventRepository.save(logEvent);
            
            // Additional processing based on log level
            if ("ERROR".equals(logEvent.getLevel())) {
                handleErrorLog(logEvent);
            }
            
            logger.info("Processed log event: {} for organization: {}", 
                    logEvent.getId(), logEvent.getOrganizationId());
            
        } catch (DataAccessException e) {
            logger.error("Database error processing log event: {}", logEvent.getId(), e);
            throw e;
        }
    }
    
    private void handleErrorLog(LogEvent logEvent) {
        // Additional error log processing (e.g., alerting, metrics)
        logger.warn("Error log detected: {} from {}", logEvent.getMessage(), logEvent.getSource());
        
        // Could trigger alerts, update error counters, etc.
        // For now, just log the error
    }
}
