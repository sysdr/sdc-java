package com.example.aggregation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacetAggregationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public void buildFacetAggregations(StreamsBuilder builder) {
        KStream<String, String> logsStream = builder.stream("logs");

        // Aggregate by service
        aggregateByField(logsStream, "service");
        
        // Aggregate by level
        aggregateByField(logsStream, "level");
        
        // Aggregate by environment
        aggregateByField(logsStream, "environment");
        
        // Aggregate by region
        aggregateByField(logsStream, "region");
        
        // Aggregate by host
        aggregateByField(logsStream, "host");
    }

    private void aggregateByField(KStream<String, String> stream, String fieldName) {
        stream
            .mapValues(value -> {
                try {
                    JsonNode node = objectMapper.readTree(value);
                    return node.get(fieldName).asText();
                } catch (Exception e) {
                    log.error("Failed to parse log", e);
                    return "unknown";
                }
            })
            .groupBy((key, value) -> value, Grouped.with(Serdes.String(), Serdes.String()))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as(fieldName + "-counts"))
            .toStream()
            .foreach((windowedKey, count) -> {
                String key = "facet:" + fieldName + ":" + windowedKey.key();
                try {
                    redisTemplate.opsForValue().set(key, count, 5, TimeUnit.MINUTES);
                    log.debug("Updated facet count: {}={}", key, count);
                } catch (Exception e) {
                    log.error("Failed to update Redis", e);
                }
            });
    }
}
