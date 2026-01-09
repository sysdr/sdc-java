package com.example.logprocessor.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private static final String TOPIC = "log-events-partitioned";

    public CompletableFuture<SendResult<String, LogEvent>> sendLogEvent(LogEvent event) {
        // Use source as partition key to ensure all logs from same source go to same partition
        ProducerRecord<String, LogEvent> record = new ProducerRecord<>(
                TOPIC,
                event.getSource(),  // Partition key
                event
        );

        CompletableFuture<SendResult<String, LogEvent>> future = kafkaTemplate.send(record);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent event {} from source {} to partition {} at offset {}",
                        event.getEventId(),
                        event.getSource(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send event {} from source {}: {}",
                        event.getEventId(),
                        event.getSource(),
                        ex.getMessage());
            }
        });

        return future;
    }

    public void sendLogEventWithCallback(LogEvent event, 
                                        java.util.function.Consumer<Integer> partitionCallback) {
        ProducerRecord<String, LogEvent> record = new ProducerRecord<>(
                TOPIC,
                event.getSource(),
                event
        );

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                partitionCallback.accept(result.getRecordMetadata().partition());
            }
        });
    }
}
