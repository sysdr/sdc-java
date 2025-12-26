package com.systemdesign.logprocessor.consumer.service;

import com.systemdesign.logprocessor.model.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private static final String DLQ_TOPIC = "application-logs-dlq";

    /**
     * Send failed messages to Dead Letter Queue for later analysis
     */
    public void sendToDLQ(ConsumerRecord<String, LogEvent> record, String reason) {
        try {
            LogEvent event = record.value();
            
            // Add failure metadata
            if (event.getMetadata() == null) {
                event.setMetadata(new java.util.HashMap<>());
            }
            event.getMetadata().put("dlq_reason", reason);
            event.getMetadata().put("original_partition", String.valueOf(record.partition()));
            event.getMetadata().put("original_offset", String.valueOf(record.offset()));
            event.getMetadata().put("dlq_timestamp", String.valueOf(System.currentTimeMillis()));
            
            kafkaTemplate.send(DLQ_TOPIC, record.key(), event);
            log.info("Sent message {} to DLQ: {}", record.key(), reason);
            
        } catch (Exception e) {
            log.error("Failed to send message to DLQ", e);
            // In production, might write to persistent error log or alert system
        }
    }
}
