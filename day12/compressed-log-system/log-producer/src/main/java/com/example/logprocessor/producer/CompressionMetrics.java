package com.example.logprocessor.producer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CompressionMetrics {

    private final MeterRegistry registry;

    public CompressionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCompression(long originalSize, long compressedSize, 
                                   long latencyNanos, String algorithm) {
        // Compression ratio gauge
        double ratio = originalSize > 0 ? 
            1.0 - ((double) compressedSize / originalSize) : 0.0;
        
        registry.gauge("compression.ratio", 
            io.micrometer.core.instrument.Tags.of("algorithm", algorithm), ratio);

        // Bytes saved counter
        Counter.builder("compression.bytes_saved")
            .tag("algorithm", algorithm)
            .register(registry)
            .increment(originalSize - compressedSize);

        // Original bytes counter
        Counter.builder("compression.bytes_original")
            .tag("algorithm", algorithm)
            .register(registry)
            .increment(originalSize);

        // Compressed bytes counter
        Counter.builder("compression.bytes_compressed")
            .tag("algorithm", algorithm)
            .register(registry)
            .increment(compressedSize);

        // Latency timer
        Timer.builder("compression.latency")
            .tag("algorithm", algorithm)
            .register(registry)
            .record(latencyNanos, TimeUnit.NANOSECONDS);
    }
}
