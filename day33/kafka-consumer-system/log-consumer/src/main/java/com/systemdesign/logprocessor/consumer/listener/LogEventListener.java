package com.systemdesign.logprocessor.consumer.listener;

import com.systemdesign.logprocessor.consumer.service.LogProcessingService;
import com.systemdesign.logprocessor.model.LogEvent;
import com.systemdesign.logprocessor.util.MetricsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventListener {

    private final LogProcessingService processingService;
    private final MetricsHelper metricsHelper;

    /**
     * Main Kafka listener for processing log events in batches.
     * Uses manual acknowledgment for at-least-once delivery guarantee.
     * Processes up to 500 records per poll with 3 concurrent threads.
     */
    @KafkaListener(
        topics = "${spring.kafka.consumer.topic}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeLogs(
        List<ConsumerRecord<String, LogEvent>> records,
        Acknowledgment acknowledgment
    ) {
        long startTime = System.currentTimeMillis();
        log.info("📥 Received batch of {} log events", records.size());
        
        try {
            // Process entire batch
            int successCount = processingService.processBatch(records);
            
            // Commit offsets only after successful processing
            acknowledgment.acknowledge();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Processed batch: {} succeeded, {} ms", successCount, duration);
            
            // Record metrics
            metricsHelper.recordProcessingTime("batch_processing", duration);
            metricsHelper.incrementCounter("logs.processed.total", 
                "status", "success",
                "count", String.valueOf(successCount));
            
        } catch (Exception e) {
            log.error("❌ Batch processing failed", e);
            metricsHelper.incrementCounter("logs.processed.total", 
                "status", "error");
            
            // On batch failure, we don't acknowledge
            // This causes reprocessing of the entire batch on next poll
            // Individual message failures are handled in processBatch()
        }
    }
}
