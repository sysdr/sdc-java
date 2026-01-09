package com.example.logprocessor.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ConsumerMetricsService {

    private final MeterRegistry meterRegistry;
    private final KafkaListenerEndpointRegistry registry;
    private final Map<Integer, Long> lastRecordedLag = new ConcurrentHashMap<>();
    
    private final Counter processedCounter;
    private final Counter failedCounter;
    private final Timer processingTimer;

    public ConsumerMetricsService(MeterRegistry meterRegistry, 
                                 KafkaListenerEndpointRegistry registry) {
        this.meterRegistry = meterRegistry;
        this.registry = registry;
        
        this.processedCounter = Counter.builder("log.events.processed")
                .description("Total number of log events processed")
                .register(meterRegistry);
                
        this.failedCounter = Counter.builder("log.events.failed")
                .description("Total number of failed log events")
                .register(meterRegistry);
                
        this.processingTimer = Timer.builder("log.processing.time")
                .description("Log event processing time")
                .register(meterRegistry);
    }

    public void recordProcessedEvent(int partition, long processingTimeMs) {
        processedCounter.increment();
        processingTimer.record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        meterRegistry.counter("log.events.processed.by.partition", 
                             "partition", String.valueOf(partition))
                    .increment();
    }

    public void recordFailedEvent(int partition) {
        failedCounter.increment();
        meterRegistry.counter("log.events.failed.by.partition",
                             "partition", String.valueOf(partition))
                    .increment();
    }

    @Scheduled(fixedRate = 30000)
    public void monitorConsumerLag() {
        for (MessageListenerContainer container : registry.getAllListenerContainers()) {
            try {
                container.getAssignedPartitions().forEach(partition -> {
                    Consumer<?, ?> consumer = getConsumer(container);
                    if (consumer != null) {
                        calculateAndRecordLag(consumer, partition);
                    }
                });
            } catch (Exception e) {
                log.error("Error monitoring consumer lag: {}", e.getMessage());
            }
        }
    }

    private void calculateAndRecordLag(Consumer<?, ?> consumer, TopicPartition partition) {
        try {
            OffsetAndMetadata committed = consumer.committed(partition);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(
                java.util.Collections.singleton(partition));
            
            if (committed != null && endOffsets.containsKey(partition)) {
                long currentOffset = committed.offset();
                long endOffset = endOffsets.get(partition);
                long lag = endOffset - currentOffset;
                
                lastRecordedLag.put(partition.partition(), lag);
                
                meterRegistry.gauge("kafka.consumer.lag",
                        java.util.Collections.singletonList(
                            io.micrometer.core.instrument.Tag.of("partition", 
                                String.valueOf(partition.partition()))),
                        lag);
                
                if (lag > 10000) {
                    log.warn("High lag detected on partition {}: {} messages", 
                            partition.partition(), lag);
                }
            }
        } catch (Exception e) {
            log.error("Error calculating lag for partition {}: {}", 
                     partition.partition(), e.getMessage());
        }
    }

    private Consumer<?, ?> getConsumer(MessageListenerContainer container) {
        // Simplified consumer extraction
        return null; // Actual implementation would extract from container
    }

    public Map<Integer, Long> getCurrentLag() {
        return new HashMap<>(lastRecordedLag);
    }
}
