package com.example.streamprocessor;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.*;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/metrics")
public class InteractiveQueryController {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public InteractiveQueryController(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    @GetMapping("/requests/current")
    public Map<String, Long> getCurrentRequestCounts() {
        try {
            KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
            if (streams == null) {
                return Collections.emptyMap();
            }

            ReadOnlyWindowStore<String, Long> store = streams.store(
                StoreQueryParameters.fromNameAndType(
                    "request-counts-store",
                    QueryableStoreTypes.windowStore()
                )
            );

            Map<String, Long> results = new HashMap<>();
            Instant now = Instant.now();
            Instant start = now.minus(java.time.Duration.ofMinutes(5));

            try (KeyValueIterator<Windowed<String>, Long> iterator = store.all()) {
                while (iterator.hasNext()) {
                    KeyValue<Windowed<String>, Long> kv = iterator.next();
                    if (kv.key.window().endTime().isAfter(start)) {
                        results.merge(kv.key.key(), kv.value, Long::sum);
                    }
                }
            }

            return results;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @GetMapping("/endpoint/{endpoint}")
    public Map<String, Object> getEndpointMetrics(@PathVariable String endpoint) {
        try {
            KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
            if (streams == null) {
                return Collections.emptyMap();
            }

            ReadOnlyWindowStore<String, EndpointMetrics> store = streams.store(
                StoreQueryParameters.fromNameAndType(
                    "endpoint-metrics-store",
                    QueryableStoreTypes.windowStore()
                )
            );

            Instant now = Instant.now();
            Instant start = now.minus(java.time.Duration.ofMinutes(10));

            List<EndpointMetrics> metricsList = new ArrayList<>();
            
            try (WindowStoreIterator<EndpointMetrics> iterator = 
                    store.fetch(endpoint, start, now)) {
                while (iterator.hasNext()) {
                    KeyValue<Long, EndpointMetrics> kv = iterator.next();
                    EndpointMetrics metrics = kv.value;
                    metrics.setWindowStart(kv.key);
                    metricsList.add(metrics);
                }
            }

            return Map.of(
                "endpoint", endpoint,
                "metrics", metricsList,
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/errors")
    public Map<String, Long> getErrorCounts() {
        try {
            KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
            if (streams == null) {
                return Collections.emptyMap();
            }

            ReadOnlyWindowStore<String, Long> store = streams.store(
                StoreQueryParameters.fromNameAndType(
                    "status-code-counts-store",
                    QueryableStoreTypes.windowStore()
                )
            );

            Map<String, Long> results = new HashMap<>();
            Instant now = Instant.now();
            Instant start = now.minus(java.time.Duration.ofMinutes(5));

            try (KeyValueIterator<Windowed<String>, Long> iterator = store.all()) {
                while (iterator.hasNext()) {
                    KeyValue<Windowed<String>, Long> kv = iterator.next();
                    if (kv.key.window().endTime().isAfter(start)) {
                        String statusCode = kv.key.key();
                        if (statusCode.startsWith("4") || statusCode.startsWith("5")) {
                            results.merge(statusCode, kv.value, Long::sum);
                        }
                    }
                }
            }

            return results;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @GetMapping("/regions")
    public Map<String, Long> getRegionalMetrics() {
        try {
            KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
            if (streams == null) {
                return Collections.emptyMap();
            }

            ReadOnlyWindowStore<String, Long> store = streams.store(
                StoreQueryParameters.fromNameAndType(
                    "regional-counts-store",
                    QueryableStoreTypes.windowStore()
                )
            );

            Map<String, Long> results = new HashMap<>();
            Instant now = Instant.now();
            Instant start = now.minus(java.time.Duration.ofMinutes(5));

            try (KeyValueIterator<Windowed<String>, Long> iterator = store.all()) {
                while (iterator.hasNext()) {
                    KeyValue<Windowed<String>, Long> kv = iterator.next();
                    if (kv.key.window().endTime().isAfter(start)) {
                        results.merge(kv.key.key(), kv.value, Long::sum);
                    }
                }
            }

            return results;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        if (streams == null) {
            return Map.of("status", "DOWN", "reason", "Streams not initialized");
        }
        
        return Map.of(
            "status", streams.state().isRunningOrRebalancing() ? "UP" : "DOWN",
            "state", streams.state().name()
        );
    }
}
