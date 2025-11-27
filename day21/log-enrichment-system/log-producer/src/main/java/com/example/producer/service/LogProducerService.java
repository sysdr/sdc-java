package com.example.producer.service;

import com.example.producer.model.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogProducerService {
    
    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private static final String TOPIC = "raw-logs";
    
    public LogProducerService(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendLog(LogEvent logEvent) {
        log.info("Sending log to Kafka: {}", logEvent.getId());
        kafkaTemplate.send(TOPIC, logEvent.getId(), logEvent);
    }
}
