package com.example.logprocessor.service;

import com.example.logprocessor.model.LogEvent;
import com.example.logprocessor.model.PriorityLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class LogProducerService {
    private static final Logger logger = LoggerFactory.getLogger(LogProducerService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PriorityClassifier priorityClassifier;
    private final ObjectMapper objectMapper;
    private final Map<PriorityLevel, Counter> priorityCounters;
    
    public LogProducerService(KafkaTemplate<String, String> kafkaTemplate,
                             PriorityClassifier priorityClassifier,
                             ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.priorityClassifier = priorityClassifier;
        this.objectMapper = objectMapper;
        
        // Initialize metrics for each priority level
        this.priorityCounters = new EnumMap<>(PriorityLevel.class);
        for (PriorityLevel level : PriorityLevel.values()) {
            priorityCounters.put(level, 
                Counter.builder("logs.produced")
                    .tag("priority", level.name())
                    .register(meterRegistry));
        }
    }
    
    public void sendLog(LogEvent event) {
        try {
            // Classify priority
            PriorityLevel priority = priorityClassifier.classify(event);
            event.setPriority(priority);
            
            // Convert to JSON
            String message = objectMapper.writeValueAsString(event);
            
            // Send to priority-specific topic
            String topic = priority.getTopicName();
            kafkaTemplate.send(topic, event.getId(), message);
            
            // Update metrics
            priorityCounters.get(priority).increment();
            
            logger.debug("Sent log {} to topic {} with priority {}", 
                event.getId(), topic, priority);
                
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize log event", e);
        }
    }
    
    /**
     * Generate random logs at 100 events/second for testing
     */
    @Scheduled(fixedRate = 10)
    public void generateRandomLog() {
        LogEvent event = LogEvent.generateRandom();
        sendLog(event);
    }
}
