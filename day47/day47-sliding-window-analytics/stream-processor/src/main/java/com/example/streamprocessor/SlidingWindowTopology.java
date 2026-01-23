package com.example.streamprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

@Configuration
public class SlidingWindowTopology {
    private static final Logger logger = LoggerFactory.getLogger(SlidingWindowTopology.class);
    
    private final ObjectMapper objectMapper;
    private final Counter processedCounter;
    
    public SlidingWindowTopology(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.processedCounter = meterRegistry.counter("stream.events.processed");
    }
    
    @Bean
    public KStream<String, LogEvent> buildTopology(StreamsBuilder streamsBuilder) {
        // Define window configurations
        Duration oneMinWindow = Duration.ofMinutes(1);
        Duration fiveMinWindow = Duration.ofMinutes(5);
        Duration fifteenMinWindow = Duration.ofMinutes(15);
        Duration hopInterval = Duration.ofSeconds(10);
        
        // Hopping windows with 10-second hops
        TimeWindows oneMinHopping = TimeWindows.ofSizeAndGrace(oneMinWindow, Duration.ofSeconds(30))
            .advanceBy(hopInterval);
        TimeWindows fiveMinHopping = TimeWindows.ofSizeAndGrace(fiveMinWindow, Duration.ofSeconds(30))
            .advanceBy(hopInterval);
        TimeWindows fifteenMinHopping = TimeWindows.ofSizeAndGrace(fifteenMinWindow, Duration.ofSeconds(30))
            .advanceBy(hopInterval);
        
        // Configure serdes
        JsonSerde<LogEvent> logEventSerde = new JsonSerde<>(LogEvent.class, objectMapper);
        JsonSerde<WindowStats> windowStatsSerde = new JsonSerde<>(WindowStats.class, objectMapper);
        
        // Build stream
        KStream<String, LogEvent> logStream = streamsBuilder
            .stream("log-events", Consumed.with(Serdes.String(), logEventSerde))
            .peek((key, value) -> {
                processedCounter.increment();
                logger.debug("Processing event for service: {}", key);
            });
        
        // 1-minute hopping windows
        logStream
            .groupByKey()
            .windowedBy(oneMinHopping)
            .aggregate(
                WindowStats::new,
                (key, event, stats) -> stats.update(event),
                Materialized.<String, WindowStats, WindowStore<Bytes, byte[]>>as("one-min-windows")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(windowStatsSerde)
                    .withRetention(Duration.ofMinutes(5))
            )
            .toStream()
            .peek((key, stats) -> 
                logger.debug("1-min window for {}: avg_latency={}ms, count={}", 
                    key.key(), stats.getAvgLatency(), stats.getCount())
            );
        
        // 5-minute hopping windows
        logStream
            .groupByKey()
            .windowedBy(fiveMinHopping)
            .aggregate(
                WindowStats::new,
                (key, event, stats) -> stats.update(event),
                Materialized.<String, WindowStats, WindowStore<Bytes, byte[]>>as("five-min-windows")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(windowStatsSerde)
                    .withRetention(Duration.ofMinutes(15))
            )
            .toStream()
            .peek((key, stats) -> 
                logger.debug("5-min window for {}: avg_latency={}ms, count={}", 
                    key.key(), stats.getAvgLatency(), stats.getCount())
            );
        
        // 15-minute hopping windows
        logStream
            .groupByKey()
            .windowedBy(fifteenMinHopping)
            .aggregate(
                WindowStats::new,
                (key, event, stats) -> stats.update(event),
                Materialized.<String, WindowStats, WindowStore<Bytes, byte[]>>as("fifteen-min-windows")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(windowStatsSerde)
                    .withRetention(Duration.ofMinutes(30))
            )
            .toStream()
            .peek((key, stats) -> 
                logger.debug("15-min window for {}: avg_latency={}ms, count={}", 
                    key.key(), stats.getAvgLatency(), stats.getCount())
            );
        
        return logStream;
    }
}
