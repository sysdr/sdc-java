package com.example.streamprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.apache.kafka.common.serialization.Serde;

import java.time.Duration;

@Configuration
@EnableKafkaStreams
public class StreamsConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public KStream<String, LogEvent> logEventStream(StreamsBuilder builder) {
        // Create a custom deserializer that extends JsonDeserializer and bypasses type checking
        JsonDeserializer<LogEvent> logEventDeserializer = new JsonDeserializer<LogEvent>(LogEvent.class, objectMapper) {
            @Override
            public LogEvent deserialize(String topic, org.apache.kafka.common.header.Headers headers, byte[] data) {
                if (data == null) {
                    return null;
                }
                try {
                    // Directly deserialize to LogEvent.class, completely bypassing type mapper
                    return objectMapper.readValue(data, LogEvent.class);
                } catch (Exception e) {
                    throw new SerializationException("Error deserializing LogEvent", e);
                }
            }
        };
        // Disable type headers as additional safety
        logEventDeserializer.setUseTypeHeaders(false);
        
        // Create and configure serializer
        JsonSerializer<LogEvent> logEventSerializer = new JsonSerializer<>(objectMapper);
        logEventSerializer.setAddTypeInfo(false);
        
        // Create JsonSerde from configured serializer and deserializer
        JsonSerde<LogEvent> logEventSerde = new JsonSerde<>(logEventSerializer, logEventDeserializer);
        
        DefaultJackson2JavaTypeMapper metricsTypeMapper = new DefaultJackson2JavaTypeMapper();
        metricsTypeMapper.addTrustedPackages("*");
        JsonDeserializer<EndpointMetrics> metricsDeserializer = new JsonDeserializer<>(EndpointMetrics.class, objectMapper);
        metricsDeserializer.setTypeMapper(metricsTypeMapper);
        JsonSerializer<EndpointMetrics> metricsSerializer = new JsonSerializer<>(objectMapper);
        JsonSerde<EndpointMetrics> metricsSerde = new JsonSerde<>(metricsSerializer, metricsDeserializer);

        // Input stream
        KStream<String, LogEvent> events = builder.stream("log-events",
            Consumed.with(Serdes.String(), logEventSerde));

        // 1. Request count per endpoint per minute (tumbling window)
        events
            .groupByKey(Grouped.with(Serdes.String(), logEventSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
            .count(Materialized.as("request-counts-store"))
            .toStream()
            .to("endpoint-request-counts");

        // 2. Detailed metrics per endpoint (1-minute tumbling window)
        events
            .groupByKey(Grouped.with(Serdes.String(), logEventSerde))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
            .aggregate(
                () -> EndpointMetrics.builder().build(),
                (key, event, metrics) -> {
                    if (metrics.getEndpoint() == null) {
                        metrics.setEndpoint(key);
                    }
                    metrics.addRequest(event);
                    return metrics;
                },
                Materialized.<String, EndpointMetrics>as(
                    Stores.persistentWindowStore(
                        "endpoint-metrics-store",
                        Duration.ofMinutes(60),
                        Duration.ofMinutes(1),
                        false
                    ))
                    .withKeySerde(Serdes.String())
                    .withValueSerde(metricsSerde)
            )
            .toStream()
            .mapValues(metrics -> {
                metrics.calculateMetrics();
                return metrics;
            })
            .to("endpoint-metrics", Produced.with(
                WindowedSerdes.timeWindowedSerdeFrom(String.class, 60000L),
                metricsSerde
            ));

        // 3. Error rate by status code (5-minute hopping window)
        events
            .groupBy(
                (key, event) -> event.getStatusCode().toString(),
                Grouped.with(Serdes.String(), logEventSerde)
            )
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("status-code-counts-store"))
            .toStream()
            .to("status-code-counts");

        // 4. Regional metrics
        events
            .groupBy(
                (key, event) -> event.getRegion(),
                Grouped.with(Serdes.String(), logEventSerde)
            )
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
            .count(Materialized.as("regional-counts-store"))
            .toStream()
            .to("regional-request-counts");

        return events;
    }
}
