package com.example.logproducer.infrashipper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsGenerator {
    
    private final KafkaTemplate<String, MetricsEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final Random random = new Random();
    
    @Scheduled(fixedRate = 1000)  // Generate 1000 metrics per second
    public void generateMetrics() {
        for (int i = 0; i < 1000; i++) {
            generateCPUMetric();
            generateMemoryMetric();
            generateDiskMetric();
        }
    }
    
    private void generateCPUMetric() {
        MetricsEvent event = MetricsEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .hostname("server-" + (random.nextInt(100) + 1))
            .metricType("cpu.usage")
            .value(random.nextDouble() * 100)
            .unit("percent")
            .timestamp(Instant.now())
            .tags(Map.of(
                "datacenter", "us-east-1",
                "environment", "production"
            ))
            .build();
        
        sendMetric(event);
    }
    
    private void generateMemoryMetric() {
        MetricsEvent event = MetricsEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .hostname("server-" + (random.nextInt(100) + 1))
            .metricType("memory.usage")
            .value(random.nextDouble() * 16384)
            .unit("MB")
            .timestamp(Instant.now())
            .tags(Map.of(
                "datacenter", "us-east-1",
                "environment", "production"
            ))
            .build();
        
        sendMetric(event);
    }
    
    private void generateDiskMetric() {
        MetricsEvent event = MetricsEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .hostname("server-" + (random.nextInt(100) + 1))
            .metricType("disk.usage")
            .value(random.nextDouble() * 1000)
            .unit("GB")
            .timestamp(Instant.now())
            .tags(Map.of(
                "datacenter", "us-east-1",
                "environment", "production"
            ))
            .build();
        
        sendMetric(event);
    }
    
    private void sendMetric(MetricsEvent event) {
        kafkaTemplate.send("infrastructure-metrics", event.getEventId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    Counter.builder("metrics.sent")
                        .tag("type", event.getMetricType())
                        .register(meterRegistry)
                        .increment();
                } else {
                    log.error("Failed to send metric", ex);
                    Counter.builder("metrics.failed")
                        .tag("type", event.getMetricType())
                        .register(meterRegistry)
                        .increment();
                }
            });
    }
}
