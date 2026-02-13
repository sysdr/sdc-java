package com.example.producer.service;

import com.example.producer.model.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {
    
    private static final String TOPIC = "raw-logs";
    
    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    
    public void sendLogEvent(LogEvent logEvent) {
        CompletableFuture<SendResult<String, LogEvent>> future = 
            kafkaTemplate.send(TOPIC, logEvent.getEventId(), logEvent);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Sent log event: {} to partition: {}", 
                    logEvent.getEventId(), 
                    result.getRecordMetadata().partition());
            } else {
                log.error("Failed to send log event: {}", logEvent.getEventId(), ex);
            }
        });
    }
}
