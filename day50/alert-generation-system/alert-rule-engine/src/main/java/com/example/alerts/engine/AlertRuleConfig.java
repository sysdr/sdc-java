package com.example.alerts.engine;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class AlertRuleConfig {

    @Bean
    public KStream<String, Alert> alertRuleStream(StreamsBuilder builder) {
        // Create state store for error counts
        builder.addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("error-count-store"),
                Serdes.String(),
                Serdes.Long()
            )
        );

        // Consume log events
        KStream<String, LogEvent> logStream = builder
            .stream("log-events", Consumed.with(Serdes.String(), new JsonSerde<>(LogEvent.class)));

        // Rule 1: Error Threshold Alert - Count errors in 5-minute windows
        KStream<String, Alert> errorThresholdAlerts = logStream
            .filter((key, event) -> "ERROR".equals(event.getLevel()))
            .groupBy((key, event) -> event.getService())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("error-count-by-service"))
            .toStream()
            .filter((windowed, count) -> count > 100)
            .map((windowed, count) -> {
                String service = windowed.key();
                Alert alert = Alert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .ruleId("error-threshold")
                    .ruleName("Error Threshold Exceeded")
                    .service(service)
                    .severity("CRITICAL")
                    .message(String.format("Service %s has %d errors in 5 minutes", service, count))
                    .count(count)
                    .timestamp(String.valueOf(System.currentTimeMillis()))
                    .fingerprint(generateFingerprint("error-threshold", service))
                    .build();
                return new KeyValue<>(service, alert);
            });

        // Rule 2: High Latency Alert - P99 latency exceeds threshold
        KStream<String, Alert> latencyAlerts = logStream
            .filter((key, event) -> event.getResponseTime() != null && event.getResponseTime() > 2000)
            .groupBy((key, event) -> event.getService())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("high-latency-count"))
            .toStream()
            .filter((windowed, count) -> count > 50)
            .map((windowed, count) -> {
                String service = windowed.key();
                Alert alert = Alert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .ruleId("high-latency")
                    .ruleName("High Latency Detected")
                    .service(service)
                    .severity("WARNING")
                    .message(String.format("Service %s has %d high-latency requests (>2s)", service, count))
                    .count(count)
                    .timestamp(String.valueOf(System.currentTimeMillis()))
                    .fingerprint(generateFingerprint("high-latency", service))
                    .build();
                return new KeyValue<>(service, alert);
            });

        // Rule 3: 5xx Error Rate Alert
        KStream<String, Alert> serverErrorAlerts = logStream
            .filter((key, event) -> event.getStatusCode() != null && event.getStatusCode() >= 500)
            .groupBy((key, event) -> event.getService())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("server-error-count"))
            .toStream()
            .filter((windowed, count) -> count > 20)
            .map((windowed, count) -> {
                String service = windowed.key();
                Alert alert = Alert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .ruleId("server-error-rate")
                    .ruleName("High 5xx Error Rate")
                    .service(service)
                    .severity("CRITICAL")
                    .message(String.format("Service %s has %d server errors (5xx) in 5 minutes", service, count))
                    .count(count)
                    .timestamp(String.valueOf(System.currentTimeMillis()))
                    .fingerprint(generateFingerprint("server-error-rate", service))
                    .build();
                return new KeyValue<>(service, alert);
            });

        // Merge all alert streams
        KStream<String, Alert> allAlerts = errorThresholdAlerts
            .merge(latencyAlerts)
            .merge(serverErrorAlerts);

        // Send alerts to alerts topic
        allAlerts.to("alerts", Produced.with(Serdes.String(), new JsonSerde<>(Alert.class)));

        return allAlerts;
    }

    private String generateFingerprint(String ruleId, String service) {
        return String.format("%s:%s", ruleId, service);
    }
}
