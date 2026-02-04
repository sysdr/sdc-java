package com.example.gateway;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class LagMonitoringService {

    private final AdminClient adminClient;
    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    
    private static final long WARNING_LAG = 10000;
    private static final long CRITICAL_LAG = 50000;
    private static final int BASE_RATE = 1000;
    private static final int MIN_RATE = 100;
    
    private volatile long currentLag = 0;

    public LagMonitoringService(AdminClient adminClient, 
                                RateLimiter rateLimiter,
                                MeterRegistry meterRegistry) {
        this.adminClient = adminClient;
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        
        // Register lag gauge
        Gauge.builder("kafka.consumer.lag", this, LagMonitoringService::getCurrentLag)
            .description("Current Kafka consumer lag")
            .register(meterRegistry);
    }

    @Scheduled(fixedRate = 5000)
    public void monitorLagAndAdjustRate() {
        try {
            long lag = calculateConsumerLag();
            currentLag = lag;
            
            int newRate = calculateAdaptiveRate(lag);
            rateLimiter.changeLimitForPeriod(newRate);
            
            if (lag > CRITICAL_LAG) {
                log.error("CRITICAL: Consumer lag {} exceeds threshold {}", lag, CRITICAL_LAG);
            } else if (lag > WARNING_LAG) {
                log.warn("WARNING: Consumer lag {} exceeds threshold {}", lag, WARNING_LAG);
            }
            
            log.debug("Consumer lag: {}, Rate limit adjusted to: {} req/s", lag, newRate);
            
        } catch (Exception e) {
            log.error("Error monitoring consumer lag", e);
        }
    }

    private long calculateConsumerLag() throws ExecutionException, InterruptedException {
        String groupId = "log-consumer-group";
        
        ListConsumerGroupOffsetsResult offsetsResult = 
            adminClient.listConsumerGroupOffsets(groupId);
        
        Map<TopicPartition, OffsetAndMetadata> offsets = 
            offsetsResult.partitionsToOffsetAndMetadata().get();
        
        Map<TopicPartition, Long> endOffsets = 
            adminClient.listOffsets(
                offsets.keySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        tp -> tp,
                        tp -> org.apache.kafka.clients.admin.OffsetSpec.latest()
                    ))
            ).all().get()
            .entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().offset()
            ));
        
        return offsets.entrySet().stream()
            .mapToLong(entry -> {
                TopicPartition tp = entry.getKey();
                long committed = entry.getValue().offset();
                long end = endOffsets.getOrDefault(tp, 0L);
                return Math.max(0, end - committed);
            })
            .sum();
    }

    private int calculateAdaptiveRate(long lag) {
        if (lag > CRITICAL_LAG) {
            return MIN_RATE;  // Severe backpressure
        }
        
        if (lag < WARNING_LAG) {
            return BASE_RATE;  // Normal operation
        }
        
        // Proportional adjustment between WARNING and CRITICAL
        double lagRatio = (double)(lag - WARNING_LAG) / (CRITICAL_LAG - WARNING_LAG);
        int adjustedRate = (int)(BASE_RATE * (1 - lagRatio * 0.7));
        
        return Math.max(MIN_RATE, adjustedRate);
    }

    public long getCurrentLag() {
        return currentLag;
    }
}
