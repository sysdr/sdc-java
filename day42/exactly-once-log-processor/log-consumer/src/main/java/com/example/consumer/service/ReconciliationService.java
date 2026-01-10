package com.example.consumer.service;

import com.example.consumer.model.LogEvent;
import com.example.consumer.repository.ProcessedLogRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ReconciliationService {

    private final ProcessedLogRepository repository;
    private final ConsumerFactory<String, LogEvent> consumerFactory;
    private final AtomicLong divergenceGauge;
    private static final long DIVERGENCE_THRESHOLD = 1000;

    public ReconciliationService(ProcessedLogRepository repository,
                                ConsumerFactory<String, LogEvent> consumerFactory,
                                MeterRegistry meterRegistry) {
        this.repository = repository;
        this.consumerFactory = consumerFactory;
        this.divergenceGauge = new AtomicLong(0);
        
        Gauge.builder("log_consumer.state.divergence", divergenceGauge, AtomicLong::get)
                .description("Difference between Kafka offset and processed count")
                .register(meterRegistry);
    }

    /**
     * Reconcile Kafka offsets with database state every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void reconcileState() {
        log.info("Starting state reconciliation");
        
        try (Consumer<String, LogEvent> consumer = consumerFactory.createConsumer("reconciliation", "reconcile")) {
            // Get partitions for topic
            TopicPartition partition0 = new TopicPartition("raw-logs", 0);
            TopicPartition partition1 = new TopicPartition("raw-logs", 1);
            TopicPartition partition2 = new TopicPartition("raw-logs", 2);
            
            consumer.assign(java.util.Arrays.asList(partition0, partition1, partition2));
            
            // Get committed offsets
            Map<TopicPartition, OffsetAndMetadata> offsets = consumer.committed(
                    java.util.Set.of(partition0, partition1, partition2)
            );
            
            long totalKafkaOffset = 0;
            long totalDbCount = 0;
            
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                if (entry.getValue() != null) {
                    long kafkaOffset = entry.getValue().offset();
                    long dbCount = repository.countByPartition(entry.getKey().partition());
                    
                    totalKafkaOffset += kafkaOffset;
                    totalDbCount += dbCount;
                    
                    long divergence = Math.abs(kafkaOffset - dbCount);
                    
                    log.info("Partition {}: Kafka offset={}, DB count={}, divergence={}", 
                            entry.getKey().partition(), kafkaOffset, dbCount, divergence);
                    
                    if (divergence > DIVERGENCE_THRESHOLD) {
                        log.error("ALERT: Significant divergence in partition {}: {} events", 
                                entry.getKey().partition(), divergence);
                    }
                }
            }
            
            long totalDivergence = Math.abs(totalKafkaOffset - totalDbCount);
            divergenceGauge.set(totalDivergence);
            
            log.info("Reconciliation complete: Total Kafka offset={}, Total DB count={}, Total divergence={}", 
                    totalKafkaOffset, totalDbCount, totalDivergence);
            
        } catch (Exception e) {
            log.error("Reconciliation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Check processing health in last hour
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void checkProcessingHealth() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentlyProcessed = repository.countProcessedSince(oneHourAgo);
        
        log.info("Processed {} events in last hour", recentlyProcessed);
        
        if (recentlyProcessed == 0) {
            log.warn("No events processed in last hour - possible consumer lag or failure");
        }
    }
}
