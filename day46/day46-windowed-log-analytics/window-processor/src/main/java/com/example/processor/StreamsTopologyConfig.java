package com.example.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.SessionStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.Instant;

@Configuration
public class StreamsTopologyConfig {
    private static final Logger logger = LoggerFactory.getLogger(StreamsTopologyConfig.class);
    
    private final ObjectMapper objectMapper;
    private final WindowResultRepository repository;
    private final WindowResultCache cache;
    private final Counter processedCounter;
    private final Counter errorCounter;
    
    public StreamsTopologyConfig(ObjectMapper objectMapper,
                                WindowResultRepository repository,
                                WindowResultCache cache,
                                MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.cache = cache;
        this.processedCounter = Counter.builder("window.events.processed")
            .description("Total events processed in windows")
            .register(meterRegistry);
        this.errorCounter = Counter.builder("window.processing.errors")
            .description("Window processing errors")
            .register(meterRegistry);
    }
    
    @Bean
    public KStream<String, String> kStream(StreamsBuilder builder) {
        // Input stream from raw logs
        KStream<String, String> rawLogs = builder.stream(
            "raw-logs",
            Consumed.with(Serdes.String(), Serdes.String())
                .withTimestampExtractor(new LogEventTimestampExtractor())
        );
        
        // Process tumbling windows (5-minute fixed windows)
        processTumblingWindows(rawLogs);
        
        // Process hopping windows (10-minute windows, 2-minute advance)
        processHoppingWindows(rawLogs);
        
        // Process session windows (5-minute inactivity gap)
        processSessionWindows(rawLogs);
        
        return rawLogs;
    }
    
    @Bean
    public StreamsBuilder streamsBuilder() {
        return new StreamsBuilder();
    }
    
    private void processTumblingWindows(KStream<String, String> stream) {
        stream
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .windowedBy(
                TimeWindows
                    .ofSizeWithNoGrace(Duration.ofMinutes(5))
                    .advanceBy(Duration.ofMinutes(5))
            )
            .aggregate(
                () -> null,
                (key, value, aggregate) -> aggregateLogEvent(value, aggregate),
                Materialized.<String, WindowStats>as(
                    Stores.persistentWindowStore(
                        "tumbling-windows-store",
                        Duration.ofHours(24),
                        Duration.ofMinutes(5),
                        false
                    )
                )
                .withValueSerde(new JsonSerde<WindowStats>(WindowStats.class, objectMapper))
            )
            .toStream()
            .foreach((windowedKey, stats) -> {
                if (stats != null) {
                    persistWindowResult(windowedKey, stats, "TUMBLING");
                }
            });
    }
    
    private void processHoppingWindows(KStream<String, String> stream) {
        stream
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .windowedBy(
                TimeWindows
                    .ofSizeWithNoGrace(Duration.ofMinutes(10))
                    .advanceBy(Duration.ofMinutes(2))
            )
            .aggregate(
                () -> null,
                (key, value, aggregate) -> aggregateLogEvent(value, aggregate),
                Materialized.<String, WindowStats>as(
                    Stores.persistentWindowStore(
                        "hopping-windows-store",
                        Duration.ofHours(24),
                        Duration.ofMinutes(10),
                        false
                    )
                )
                .withValueSerde(new JsonSerde<WindowStats>(WindowStats.class, objectMapper))
            )
            .toStream()
            .foreach((windowedKey, stats) -> {
                if (stats != null) {
                    persistWindowResult(windowedKey, stats, "HOPPING");
                }
            });
    }
    
    private void processSessionWindows(KStream<String, String> stream) {
        stream
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .windowedBy(
                SessionWindows.ofInactivityGapWithNoGrace(Duration.ofMinutes(5))
            )
            .aggregate(
                () -> null,
                (key, value, aggregate) -> aggregateLogEvent(value, aggregate),
                (key, agg1, agg2) -> mergeWindowStats(agg1, agg2),
                Materialized.<String, WindowStats>as(
                    Stores.persistentSessionStore(
                        "session-windows-store",
                        Duration.ofHours(24)
                    )
                )
                .withValueSerde(new JsonSerde<WindowStats>(WindowStats.class, objectMapper))
            )
            .toStream()
            .foreach((windowedKey, stats) -> {
                if (stats != null) {
                    persistWindowResult(windowedKey, stats, "SESSION");
                }
            });
    }
    
    private WindowStats aggregateLogEvent(String logJson, WindowStats currentStats) {
        try {
            JsonNode log = objectMapper.readTree(logJson);
            String level = log.get("level").asText();
            int latency = log.get("latency_ms").asInt();
            
            processedCounter.increment();
            return WindowStats.aggregate(currentStats, level, latency);
            
        } catch (Exception e) {
            errorCounter.increment();
            logger.error("Failed to parse log event: {}", e.getMessage());
            return currentStats;
        }
    }
    
    private WindowStats mergeWindowStats(WindowStats stats1, WindowStats stats2) {
        if (stats1 == null) return stats2;
        if (stats2 == null) return stats1;
        
        stats1.setEventCount(stats1.getEventCount() + stats2.getEventCount());
        stats1.setErrorCount(stats1.getErrorCount() + stats2.getErrorCount());
        stats1.setWarnCount(stats1.getWarnCount() + stats2.getWarnCount());
        stats1.setTotalLatency(stats1.getTotalLatency() + stats2.getTotalLatency());
        stats1.setMaxLatency(Math.max(stats1.getMaxLatency(), stats2.getMaxLatency()));
        stats1.setMinLatency(Math.min(stats1.getMinLatency(), stats2.getMinLatency()));
        stats1.getLatencies().addAll(stats2.getLatencies());
        
        return stats1;
    }
    
    private void persistWindowResult(Windowed<String> windowedKey, WindowStats stats, String windowType) {
        try {
            WindowResult result = WindowResult.builder()
                .windowKey(windowedKey.key())
                .windowStart(windowedKey.window().start())
                .windowEnd(windowedKey.window().end())
                .windowType(windowType)
                .eventCount(stats.getEventCount())
                .errorCount(stats.getErrorCount())
                .warnCount(stats.getWarnCount())
                .avgLatencyMs(stats.getAvgLatency())
                .maxLatencyMs(stats.getMaxLatency())
                .minLatencyMs(stats.getMinLatency())
                .p95LatencyMs(stats.getP95Latency())
                .errorRate(stats.getErrorRate())
                .computedAt(Instant.now().toEpochMilli())
                .build();
            
            // Cache for fast API access
            cache.put(result);
            
            // Persist to PostgreSQL for historical queries
            repository.save(toEntity(result));
            
            logger.info("Window completed: {} {} [{}-{}] events={} errors={} avgLatency={}ms",
                windowType, windowedKey.key(),
                Instant.ofEpochMilli(result.getWindowStart()),
                Instant.ofEpochMilli(result.getWindowEnd()),
                result.getEventCount(),
                result.getErrorCount(),
                String.format("%.2f", result.getAvgLatencyMs())
            );
            
        } catch (Exception e) {
            logger.error("Failed to persist window result: {}", e.getMessage(), e);
        }
    }
    
    private WindowResultEntity toEntity(WindowResult result) {
        WindowResultEntity entity = new WindowResultEntity();
        entity.setWindowKey(result.getWindowKey());
        entity.setWindowStart(Instant.ofEpochMilli(result.getWindowStart()));
        entity.setWindowEnd(Instant.ofEpochMilli(result.getWindowEnd()));
        entity.setWindowType(result.getWindowType());
        entity.setEventCount(result.getEventCount());
        entity.setErrorCount(result.getErrorCount());
        entity.setWarnCount(result.getWarnCount());
        entity.setAvgLatencyMs(result.getAvgLatencyMs());
        entity.setMaxLatencyMs(result.getMaxLatencyMs());
        entity.setMinLatencyMs(result.getMinLatencyMs());
        entity.setP95LatencyMs(result.getP95LatencyMs());
        entity.setErrorRate(result.getErrorRate());
        entity.setComputedAt(Instant.ofEpochMilli(result.getComputedAt()));
        return entity;
    }
}
