package com.example.logconsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class RetryConsumerService {
    
    private static final Logger log = LoggerFactory.getLogger(RetryConsumerService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public RetryConsumerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @KafkaListener(topics = "log-events-retry", groupId = "retry-consumer-group")
    public void consumeRetryMessage(@Payload String message,
                                   @Header("retry-count") int retryCount,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        
        log.info("Reprocessing message from retry queue. Key: {}, RetryCount: {}", key, retryCount);
        
        // Forward back to main topic with retry count preserved
        kafkaTemplate.send(MessageBuilder
            .withPayload(message)
            .setHeader(KafkaHeaders.TOPIC, "log-events")
            .setHeader(KafkaHeaders.KEY, key)
            .setHeader("retry-count", retryCount)
            .build());
    }
}
