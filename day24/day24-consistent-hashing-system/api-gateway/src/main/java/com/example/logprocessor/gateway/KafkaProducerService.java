package com.example.logprocessor.gateway;

import com.example.logprocessor.common.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    
    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private static final String TOPIC = "distributed-logs";
    
    public void sendLog(LogEvent logEvent) {
        CompletableFuture<SendResult<String, LogEvent>> future = 
            kafkaTemplate.send(TOPIC, logEvent.getSourceIp(), logEvent);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Sent log {} to partition {}", 
                    logEvent.getId(), result.getRecordMetadata().partition());
            } else {
                log.error("Failed to send log {}", logEvent.getId(), ex);
            }
        });
    }
}
