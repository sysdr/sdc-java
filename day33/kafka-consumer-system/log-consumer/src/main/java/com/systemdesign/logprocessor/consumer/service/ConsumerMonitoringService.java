package com.systemdesign.logprocessor.consumer.service;

import com.systemdesign.logprocessor.model.ConsumerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerMonitoringService {

    private final KafkaListenerEndpointRegistry endpointRegistry;
    private final MeterRegistry meterRegistry;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private long totalProcessed = 0;
    private long totalFailed = 0;
    private int rebalanceCount = 0;
    private long lastRebalanceTime = System.currentTimeMillis();

    /**
     * Monitor consumer lag every 10 seconds and expose metrics
     */
    @Scheduled(fixedDelay = 10000)
    public void monitorConsumerLag() {
        try {
            Map<Integer, Long> partitionLags = new HashMap<>();
            long totalLag = 0;
            
            // Access container to get consumer instances
            var containers = endpointRegistry.getListenerContainers();
            
            for (var container : containers) {
                if (container.isRunning()) {
                    // In production, you'd get actual lag from consumer metrics
                    // This is a simplified version
                    int partitionCount = 12; // From our Kafka setup
                    for (int i = 0; i < partitionCount; i++) {
                        long lag = (long) (Math.random() * 1000); // Simulated lag
                        partitionLags.put(i, lag);
                        totalLag += lag;
                        
                        // Expose per-partition lag as Prometheus gauge
                        meterRegistry.gauge("kafka.consumer.lag",
                            io.micrometer.core.instrument.Tags.of(
                                "group", groupId,
                                "partition", String.valueOf(i)
                            ),
                            lag);
                    }
                }
            }
            
            // Alert if lag exceeds threshold
            if (totalLag > 50000) {
                log.warn("⚠️ Consumer lag exceeds threshold: {} messages", totalLag);
                meterRegistry.counter("kafka.consumer.lag.alerts").increment();
            }
            
            log.debug("Consumer lag monitoring: {} total messages behind", totalLag);
            
        } catch (Exception e) {
            log.error("Failed to monitor consumer lag", e);
        }
    }

    /**
     * Get current consumer metrics for API exposure
     */
    public ConsumerMetrics getConsumerMetrics() {
        int activeConsumers = (int) endpointRegistry.getListenerContainers()
            .stream()
            .filter(org.springframework.kafka.listener.MessageListenerContainer::isRunning)
            .count();
        
        return ConsumerMetrics.builder()
            .groupId(groupId)
            .activeConsumers(activeConsumers)
            .partitionLags(new HashMap<>()) // Would be populated from actual metrics
            .totalProcessed(totalProcessed)
            .totalFailed(totalFailed)
            .averageProcessingTimeMs(0.0) // Would be calculated from timing metrics
            .lastRebalanceTimestamp(lastRebalanceTime)
            .rebalanceCount(rebalanceCount)
            .build();
    }

    public void recordProcessed(int count) {
        totalProcessed += count;
    }

    public void recordFailed(int count) {
        totalFailed += count;
    }

    public void recordRebalance() {
        rebalanceCount++;
        lastRebalanceTime = System.currentTimeMillis();
    }
}
