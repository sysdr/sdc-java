package com.example.sessionization.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SessionStreamConfig {
    private static final Logger log = LoggerFactory.getLogger(SessionStreamConfig.class);
    private static final String SESSION_INACTIVITY_GAP = "PT30M"; // 30 minutes
    
    private final SessionCacheService cacheService;
    private final ObjectMapper objectMapper;

    public SessionStreamConfig(SessionCacheService cacheService, ObjectMapper objectMapper) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "session-processor");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024); // 10MB
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
        
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public KStream<String, SessionAggregate> sessionStream(StreamsBuilder builder) {
        JsonSerde<UserEvent> eventSerde = new JsonSerde<>(UserEvent.class, objectMapper);
        JsonSerde<SessionAggregate> sessionSerde = new JsonSerde<>(SessionAggregate.class, objectMapper);
        
        // Read user events stream
        KStream<String, String> eventsStream = builder.stream(
            "user-events",
            Consumed.with(Serdes.String(), Serdes.String())
        );
        
        // Parse JSON events
        KStream<String, UserEvent> parsedEvents = eventsStream
            .mapValues(json -> {
                try {
                    return objectMapper.readValue(json, UserEvent.class);
                } catch (Exception e) {
                    log.error("Error parsing event: {}", json, e);
                    return null;
                }
            })
            .filter((key, value) -> value != null);
        
        // Session windowing with 30-minute inactivity gap
        KTable<Windowed<String>, SessionAggregate> sessionTable = parsedEvents
            .groupByKey(Grouped.with(Serdes.String(), eventSerde))
            .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.parse(SESSION_INACTIVITY_GAP)))
            .aggregate(
                SessionAggregate::new,
                (key, event, aggregate) -> aggregate.addEvent(event),
                (key, agg1, agg2) -> agg1.merge(agg2),
                Materialized.with(Serdes.String(), sessionSerde)
            );
        
        // Convert to stream for further processing
        KStream<String, SessionAggregate> sessionsStream = sessionTable
            .toStream((windowedKey, value) -> windowedKey.key())
            .peek((key, session) -> {
                log.info("Session update for user {}: {} events, {} seconds duration, converted={}",
                    key, session.getEventCount(), session.getDurationSeconds(), session.isHasConversion());
                
                // Cache active session in Redis
                cacheService.cacheSession(key, session);
            });
        
        // Write completed sessions to output topic
        sessionsStream.to("completed-sessions", Produced.with(Serdes.String(), sessionSerde));
        
        return sessionsStream;
    }
}
