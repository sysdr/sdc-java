package com.example.queryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class QueryApiStreamsConfig {
    private static final Logger logger = LoggerFactory.getLogger(QueryApiStreamsConfig.class);
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG, applicationName);
        props.put(org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(org.apache.kafka.streams.StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams-query");
        props.put(org.apache.kafka.streams.StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(org.apache.kafka.streams.StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(org.apache.kafka.streams.StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
        
        return new KafkaStreamsConfiguration(props);
    }
    
    @Bean
    public KStream<String, LogEvent> buildQueryTopology(StreamsBuilder streamsBuilder, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        // This topology mirrors the stream processor topology for querying
        // We need to recreate the same state stores for interactive queries
        Counter processedCounter = meterRegistry.counter("query.stream.events.processed");
        
        Duration oneMinWindow = Duration.ofMinutes(1);
        Duration fiveMinWindow = Duration.ofMinutes(5);
        Duration fifteenMinWindow = Duration.ofMinutes(15);
        Duration hopInterval = Duration.ofSeconds(10);
        
        TimeWindows oneMinHopping = TimeWindows.ofSizeAndGrace(oneMinWindow, Duration.ofSeconds(30))
            .advanceBy(hopInterval);
        TimeWindows fiveMinHopping = TimeWindows.ofSizeAndGrace(fiveMinWindow, Duration.ofSeconds(30))
            .advanceBy(hopInterval);
        TimeWindows fifteenMinHopping = TimeWindows.ofSizeAndGrace(fifteenMinWindow, Duration.ofSeconds(30))
            .advanceBy(hopInterval);
        
        // Configure serdes
        JsonSerde<LogEvent> logEventSerde = new JsonSerde<>(LogEvent.class, objectMapper);
        JsonSerde<WindowStats> windowStatsSerde = new JsonSerde<>(WindowStats.class, objectMapper);
        
        // Build stream - parse JSON events
        KStream<String, LogEvent> logStream = streamsBuilder
            .stream("log-events", Consumed.with(Serdes.String(), logEventSerde))
            .peek((key, value) -> {
                processedCounter.increment();
                logger.debug("Query API processing event for service: {}", key);
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
                logger.debug("Query API 1-min window for {}: avg_latency={}ms, count={}", 
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
                logger.debug("Query API 5-min window for {}: avg_latency={}ms, count={}", 
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
                logger.debug("Query API 15-min window for {}: avg_latency={}ms, count={}", 
                    key.key(), stats.getAvgLatency(), stats.getCount())
            );
        
        return logStream;
    }
}
