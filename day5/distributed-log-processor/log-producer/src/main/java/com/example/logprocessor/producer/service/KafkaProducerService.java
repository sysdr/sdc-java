package com.example.logprocessor.producer.service;

import com.example.logprocessor.producer.model.LogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC_NAME = "log-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void sendLogEvent(LogEvent logEvent) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(logEvent);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(TOPIC_NAME, logEvent.getTraceId(), jsonMessage);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.debug("Successfully sent log event to Kafka: trace_id={}, offset={}", 
                               logEvent.getTraceId(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send log event to Kafka: trace_id={}", 
                               logEvent.getTraceId(), ex);
                }
            });

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize log event to JSON: trace_id={}", logEvent.getTraceId(), e);
            throw new RuntimeException("Failed to serialize log event", e);
        }
    }
}