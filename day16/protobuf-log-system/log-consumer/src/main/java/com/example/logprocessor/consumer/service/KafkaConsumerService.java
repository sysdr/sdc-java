package com.example.logprocessor.consumer.service;

import com.example.logprocessor.proto.LogEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumerService {
    
    private final LogStorageService logStorageService;
    private final Counter consumedCounter;
    private final Counter errorCounter;
    
    public KafkaConsumerService(LogStorageService logStorageService,
                               MeterRegistry meterRegistry) {
        this.logStorageService = logStorageService;
        this.consumedCounter = Counter.builder("kafka_messages_consumed")
            .description("Total messages consumed from Kafka")
            .register(meterRegistry);
        this.errorCounter = Counter.builder("kafka_consumption_errors")
            .description("Errors during message consumption")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "log-events-protobuf", groupId = "log-consumer-group")
    public void consumeLogEvent(@Payload byte[] message,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            // Deserialize protobuf
            LogEvent logEvent = LogEvent.parseFrom(message);
            
            consumedCounter.increment();
            
            log.debug("Consumed event {} from partition {}, offset {}", 
                logEvent.getEventId(), partition, offset);
            
            // Process and store
            logStorageService.storeLogEvent(logEvent);
            
        } catch (InvalidProtocolBufferException e) {
            errorCounter.increment();
            log.error("Failed to parse protobuf message from partition {}, offset {}: {}", 
                partition, offset, e.getMessage());
            // In production: send to dead letter queue
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error processing message from partition {}, offset {}", 
                partition, offset, e);
            // In production: implement retry logic or DLQ
        }
    }
}
