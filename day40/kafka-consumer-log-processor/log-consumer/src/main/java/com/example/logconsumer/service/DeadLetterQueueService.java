package com.example.logconsumer.service;

import com.example.logconsumer.model.DeadLetterMessage;
import com.example.logconsumer.model.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class DeadLetterQueueService {

    private static final String DLQ_TOPIC = "logs-dlq";
    private final KafkaTemplate<String, DeadLetterMessage> kafkaTemplate;
    private final Counter dlqCounter;

    public DeadLetterQueueService(KafkaTemplate<String, DeadLetterMessage> kafkaTemplate,
                                 MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.dlqCounter = Counter.builder("log.dlq.messages")
                .description("Messages sent to DLQ")
                .register(meterRegistry);
    }

    public void sendToDLQ(ConsumerRecord<String, LogEvent> record, 
                         Exception exception, 
                         int retryCount) {
        
        DeadLetterMessage dlqMessage = DeadLetterMessage.builder()
                .originalMessageId(record.value().getId())
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .failureReason(exception.getMessage())
                .exceptionClass(exception.getClass().getName())
                .retryCount(retryCount)
                .timestamp(Instant.now())
                .originalPayload(record.value())
                .build();
        
        kafkaTemplate.send(DLQ_TOPIC, record.key(), dlqMessage)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message to DLQ: {}", record.key(), ex);
                    } else {
                        dlqCounter.increment();
                        log.warn("Message sent to DLQ: id={}, reason={}", 
                                record.value().getId(), exception.getMessage());
                    }
                });
    }
}
