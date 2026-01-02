package com.example.performance;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PerformanceLogProcessor {
    
    private final Counter metricsProcessed;
    private final List<Long> responseTimes = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public PerformanceLogProcessor(MeterRegistry meterRegistry) {
        this.metricsProcessed = Counter.builder("performance.metrics.processed")
            .description("Number of performance metrics processed")
            .register(meterRegistry);
        
        // Aggregate metrics every 10 seconds
        scheduler.scheduleAtFixedRate(this::aggregateMetrics, 10, 10, TimeUnit.SECONDS);
    }
    
    @KafkaListener(topics = "logs-performance", groupId = "performance-consumer-group")
    public void processPerformanceLog(LogEvent event) {
        try {
            log.debug("Performance metric: {} from {}", event.getMessage(), event.getSource());
            
            // Extract response time from metadata
            if (event.getMetadata() != null && event.getMetadata().containsKey("responseTime")) {
                Object responseTime = event.getMetadata().get("responseTime");
                if (responseTime instanceof Number) {
                    synchronized (responseTimes) {
                        responseTimes.add(((Number) responseTime).longValue());
                    }
                }
            }
            
            metricsProcessed.increment();
        } catch (Exception e) {
            log.error("Error processing performance log", e);
        }
    }
    
    private void aggregateMetrics() {
        synchronized (responseTimes) {
            if (responseTimes.isEmpty()) {
                return;
            }
            
            LongSummaryStatistics stats = responseTimes.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();
            
            log.info("Performance Summary - Count: {}, Avg: {}ms, Min: {}ms, Max: {}ms",
                stats.getCount(), stats.getAverage(), stats.getMin(), stats.getMax());
            
            responseTimes.clear();
        }
    }
}
