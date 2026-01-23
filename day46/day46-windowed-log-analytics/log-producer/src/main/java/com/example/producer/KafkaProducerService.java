package com.example.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "raw-logs";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter sentCounter;
    private final Counter errorCounter;
    
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.sentCounter = Counter.builder("log.events.sent")
            .description("Total log events sent to Kafka")
            .register(meterRegistry);
        this.errorCounter = Counter.builder("log.events.error")
            .description("Failed log event sends")
            .register(meterRegistry);
    }
    
    public void sendLogEvent(LogEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            
            // Use service name as partition key for balanced distribution
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(TOPIC, event.getService(), json);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    sentCounter.increment();
                    logger.debug("Sent event {} to partition {}", 
                        event.getEventId(), 
                        result.getRecordMetadata().partition());
                } else {
                    errorCounter.increment();
                    logger.error("Failed to send event {}: {}", 
                        event.getEventId(), ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            errorCounter.increment();
            logger.error("Failed to serialize event {}: {}", event.getEventId(), e.getMessage());
        }
    }
}
