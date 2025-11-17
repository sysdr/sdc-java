package com.example.logprocessor.producer.service;

import com.example.logprocessor.proto.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaProducerService {
    
    private static final String TOPIC_NAME = "log-events-protobuf";
    
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final Counter successCounter;
    private final Counter failureCounter;
    
    public KafkaProducerService(KafkaTemplate<String, byte[]> kafkaTemplate,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.successCounter = Counter.builder("kafka_publish_success")
            .description("Successful Kafka publishes")
            .register(meterRegistry);
        this.failureCounter = Counter.builder("kafka_publish_failure")
            .description("Failed Kafka publishes")
            .register(meterRegistry);
    }
    
    public void sendLogEvent(LogEvent logEvent) {
        try {
            // Serialize protobuf to bytes
            byte[] eventBytes = logEvent.toByteArray();
            
            // Use event ID as partition key for ordering
            String key = logEvent.getEventId();
            
            CompletableFuture<SendResult<String, byte[]>> future = 
                kafkaTemplate.send(TOPIC_NAME, key, eventBytes);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    successCounter.increment();
                    log.debug("Published event {} to partition {}, offset {}", 
                        key, 
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    failureCounter.increment();
                    log.error("Failed to publish event {}: {}", key, ex.getMessage());
                }
            });
        } catch (Exception e) {
            failureCounter.increment();
            log.error("Error serializing log event", e);
            throw new RuntimeException("Failed to send log event", e);
        }
    }
}
