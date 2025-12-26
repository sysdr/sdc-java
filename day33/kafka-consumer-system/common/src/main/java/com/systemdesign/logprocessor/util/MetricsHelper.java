package com.systemdesign.logprocessor.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class MetricsHelper {
    private final MeterRegistry registry;
    
    public MetricsHelper(MeterRegistry registry) {
        this.registry = registry;
    }
    
    public void recordProcessingTime(String operation, long durationMs) {
        Timer.builder("log.processing.duration")
            .tag("operation", operation)
            .register(registry)
            .record(java.time.Duration.ofMillis(durationMs));
    }
    
    public void incrementCounter(String name, String... tags) {
        Counter.builder(name)
            .tags(tags)
            .register(registry)
            .increment();
    }
}
