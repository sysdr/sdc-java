package com.example.logprocessor.consumer;

import com.example.logprocessor.model.LogEvent;
import com.example.logprocessor.service.LogProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NormalLogConsumer {
    private static final Logger logger = LoggerFactory.getLogger(NormalLogConsumer.class);
    
    private final LogProcessingService processingService;
    private final ObjectMapper objectMapper;
    
    public NormalLogConsumer(LogProcessingService processingService,
                            ObjectMapper objectMapper) {
        this.processingService = processingService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(
        topics = "normal-logs",
        groupId = "normal-consumer-group",
        containerFactory = "normalKafkaListenerContainerFactory"
    )
    public void consumeNormal(String message, Acknowledgment ack) {
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            event.setProcessedAt(Instant.now());
            
            processingService.processNormal(event);
            ack.acknowledge();
            
        } catch (Exception e) {
            logger.error("Failed to process normal log", e);
        }
    }
    
    @KafkaListener(
        topics = "low-logs",
        groupId = "normal-consumer-group",
        containerFactory = "normalKafkaListenerContainerFactory"
    )
    public void consumeLow(String message, Acknowledgment ack) {
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            event.setProcessedAt(Instant.now());
            
            processingService.processLow(event);
            ack.acknowledge();
            
        } catch (Exception e) {
            logger.error("Failed to process low priority log", e);
        }
    }
}
