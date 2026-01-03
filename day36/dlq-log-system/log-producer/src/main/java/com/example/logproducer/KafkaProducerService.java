package com.example.logproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "log-events";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter messagesPublished;
    
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.messagesPublished = Counter.builder("producer.messages.published")
            .description("Total messages published to Kafka")
            .register(meterRegistry);
    }
    
    public void sendLogEvent(LogEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getMessageId(), json)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        messagesPublished.increment();
                        log.debug("Published message {} to partition {}", 
                            event.getMessageId(), result.getRecordMetadata().partition());
                    } else {
                        log.error("Failed to publish message {}: {}", 
                            event.getMessageId(), ex.getMessage());
                    }
                });
        } catch (Exception e) {
            log.error("Error serializing event: {}", e.getMessage());
        }
    }
}
