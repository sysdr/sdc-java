package com.example.logconsumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeadLetterQueueService {
    
    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueService.class);
    private static final String DLQ_TOPIC = "log-events-dlq";
    private static final String RETRY_TOPIC = "log-events-retry";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<String, Counter> dlqCounters;
    
    public DeadLetterQueueService(KafkaTemplate<String, String> kafkaTemplate,
                                 MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.dlqCounters = new HashMap<>();
        
        // Initialize counters for each error type
        for (String errorType : new String[]{"VALIDATION", "TIMEOUT", "PROCESSING", "UNKNOWN"}) {
            dlqCounters.put(errorType, 
                Counter.builder("dlq.messages.total")
                    .tag("type", errorType)
                    .description("Messages in DLQ by error type")
                    .register(meterRegistry));
        }
    }
    
    public void sendToDeadLetterQueue(String originalMessage, int retryCount, 
                                     String errorType, String errorMessage, String key) {
        
        Message<String> dlqMessage = MessageBuilder
            .withPayload(originalMessage)
            .setHeader(KafkaHeaders.TOPIC, DLQ_TOPIC)
            .setHeader(KafkaHeaders.KEY, key)
            .setHeader("retry-count", retryCount)
            .setHeader("error-type", errorType)
            .setHeader("error-message", errorMessage)
            .setHeader("dlq-timestamp", System.currentTimeMillis())
            .setHeader("original-topic", "log-events")
            .build();
        
        kafkaTemplate.send(dlqMessage);
        dlqCounters.get(errorType).increment();
        
        log.info("Sent message to DLQ. Key: {}, Type: {}, RetryCount: {}", 
            key, errorType, retryCount);
    }
    
    public void sendToRetryTopic(String originalMessage, int retryCount, 
                                String errorType, String errorMessage, String key) {
        
        Message<String> retryMessage = MessageBuilder
            .withPayload(originalMessage)
            .setHeader(KafkaHeaders.TOPIC, RETRY_TOPIC)
            .setHeader(KafkaHeaders.KEY, key)
            .setHeader("retry-count", retryCount)
            .setHeader("error-type", errorType)
            .setHeader("error-message", errorMessage)
            .setHeader("retry-timestamp", System.currentTimeMillis())
            .build();
        
        // Calculate exponential backoff delay
        long delayMs = (long) Math.pow(2, retryCount) * 1000; // 2s, 4s, 8s
        
        try {
            Thread.sleep(delayMs); // Simple delay - in production use scheduled retry
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        kafkaTemplate.send(retryMessage);
        log.info("Sent message to retry topic. Key: {}, Attempt: {}, Delay: {}ms", 
            key, retryCount, delayMs);
    }
}
