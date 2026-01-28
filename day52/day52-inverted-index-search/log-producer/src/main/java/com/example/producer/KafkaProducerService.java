package com.example.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicLong counter = new AtomicLong(0);
    
    public void sendLogEvent(LogEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send("log-events", event.getId().toString(), json);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Sent log event: {} to partition: {}", 
                        event.getId(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to send log event: {}", event.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error serializing log event", e);
        }
    }
    
    public void sendBatch(int count) {
        for (int i = 0; i < count; i++) {
            LogEvent event = LogEvent.generate(counter.incrementAndGet());
            sendLogEvent(event);
        }
    }
}
