package com.example.gateway.service;

import com.example.gateway.model.LogEventRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class LogProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topic.log-events}")
    private String topicName;
    
    public LogProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    public String publishLogEvent(LogEventRequest request) throws Exception {
        String messageId = UUID.randomUUID().toString();
        
        Map<String, Object> event = new HashMap<>();
        event.put("messageId", messageId);
        event.put("level", request.getLevel());
        event.put("message", request.getMessage());
        event.put("source", request.getSource());
        event.put("timestamp", Instant.now().toEpochMilli());
        
        String json = objectMapper.writeValueAsString(event);
        
        kafkaTemplate.send(topicName, messageId, json);
        
        log.info("Published log event: {}", messageId);
        
        return messageId;
    }
}
