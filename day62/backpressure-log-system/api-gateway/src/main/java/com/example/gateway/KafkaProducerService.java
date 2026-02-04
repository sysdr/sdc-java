package com.example.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private static final String TOPIC = "log-events";

    public KafkaProducerService(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<String> sendLog(LogEvent event) {
        return Mono.fromFuture(
            kafkaTemplate.send(TOPIC, event.getCorrelationId(), event)
                .thenApply(SendResult::getRecordMetadata)
                .thenApply(metadata -> 
                    String.format("offset=%d, partition=%d", 
                        metadata.offset(), metadata.partition()))
        );
    }
}
