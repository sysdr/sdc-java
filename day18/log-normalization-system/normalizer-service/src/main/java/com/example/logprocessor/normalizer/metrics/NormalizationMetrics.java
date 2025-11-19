package com.example.logprocessor.normalizer.metrics;

import com.example.logprocessor.common.format.LogFormat;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NormalizationMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> failureCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();

    public NormalizationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordConversion(LogFormat source, LogFormat target, 
                                  boolean success, long durationMs) {
        String key = source.name() + "_to_" + target.name();

        if (success) {
            getSuccessCounter(source, target).increment();
        } else {
            getFailureCounter(source, target).increment();
        }

        getTimer(source, target).record(Duration.ofMillis(durationMs));
    }

    private Counter getSuccessCounter(LogFormat source, LogFormat target) {
        String key = source.name() + "_to_" + target.name();
        return successCounters.computeIfAbsent(key, k ->
            Counter.builder("normalization.success")
                    .tag("source", source.name())
                    .tag("target", target.name())
                    .register(meterRegistry)
        );
    }

    private Counter getFailureCounter(LogFormat source, LogFormat target) {
        String key = source.name() + "_to_" + target.name();
        return failureCounters.computeIfAbsent(key, k ->
            Counter.builder("normalization.failure")
                    .tag("source", source.name())
                    .tag("target", target.name())
                    .register(meterRegistry)
        );
    }

    private Timer getTimer(LogFormat source, LogFormat target) {
        String key = source.name() + "_to_" + target.name();
        return timers.computeIfAbsent(key, k ->
            Timer.builder("normalization.duration")
                    .tag("source", source.name())
                    .tag("target", target.name())
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
        );
    }
}
