package com.example.logprocessor.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogEventListener {

    private final LogProcessingService processingService;
    private final ConsumerMetricsService metricsService;

    @KafkaListener(
            topics = "log-events-partitioned",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeLogEvent(
            @Payload LogEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            ConsumerRecord<String, LogEvent> record,
            Acknowledgment acknowledgment) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Processing event {} from partition {} at offset {}", 
                     event.getEventId(), partition, offset);
            
            // Process the log event
            processingService.processLogEvent(event, partition);
            
            // Record successful processing
            metricsService.recordProcessedEvent(partition, 
                System.currentTimeMillis() - startTime);
            
            // Manual acknowledgment
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing event {} from partition {}: {}", 
                     event.getEventId(), partition, e.getMessage(), e);
            metricsService.recordFailedEvent(partition);
            
            // Don't acknowledge on failure - will be reprocessed
            // In production, implement dead letter queue after N retries
        }
    }
}
