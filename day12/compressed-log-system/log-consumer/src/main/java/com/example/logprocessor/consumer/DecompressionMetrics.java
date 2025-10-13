package com.example.logprocessor.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DecompressionMetrics {

    private final MeterRegistry registry;

    public DecompressionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordDecompression(long latencyNanos, String algorithm, boolean success) {
        Timer.builder("decompression.latency")
            .tag("algorithm", algorithm)
            .tag("success", String.valueOf(success))
            .register(registry)
            .record(latencyNanos, TimeUnit.NANOSECONDS);

        if (!success) {
            Counter.builder("decompression.failures")
                .tag("algorithm", algorithm)
                .register(registry)
                .increment();
        }
    }

    public void recordCircuitBreakerOpen(String algorithm) {
        Counter.builder("decompression.circuit_breaker_open")
            .tag("algorithm", algorithm)
            .register(registry)
            .increment();
    }
}
