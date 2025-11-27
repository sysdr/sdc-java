package com.example.enrichment.config;

import com.example.enrichment.model.EnrichedLogEvent;
import com.example.enrichment.model.LogEvent;
import com.example.enrichment.service.EnrichmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;

@Configuration
@Slf4j
public class KafkaStreamsConfig {
    
    private final EnrichmentService enrichmentService;
    private final ObjectMapper objectMapper;
    
    public KafkaStreamsConfig(EnrichmentService enrichmentService, ObjectMapper objectMapper) {
        this.enrichmentService = enrichmentService;
        this.objectMapper = objectMapper;
    }
    
    @Bean
    public KStream<String, EnrichedLogEvent> enrichmentStream(StreamsBuilder builder) {
        
        JsonSerde<LogEvent> logEventSerde = new JsonSerde<>(LogEvent.class, objectMapper);
        JsonSerde<EnrichedLogEvent> enrichedLogEventSerde = 
            new JsonSerde<>(EnrichedLogEvent.class, objectMapper);
        
        // Source: raw logs
        KStream<String, LogEvent> rawLogs = builder.stream(
            "raw-logs",
            Consumed.with(Serdes.String(), logEventSerde)
        );
        
        // Stage 1: Filter invalid logs
        KStream<String, LogEvent> validLogs = rawLogs
            .filter((key, logEvent) -> {
                boolean valid = logEvent != null && logEvent.getTimestamp() != null;
                if (!valid) {
                    log.warn("Filtered invalid log with key: {}", key);
                }
                return valid;
            });
        
        // Stage 2: Enrich logs
        KStream<String, EnrichedLogEvent> enrichedLogs = validLogs
            .mapValues(logEvent -> {
                log.info("Enriching log: {}", logEvent.getId());
                return enrichmentService.enrich(logEvent);
            });
        
        // Stage 3: Branch into fully enriched and partially enriched
        Map<String, KStream<String, EnrichedLogEvent>> branches = enrichedLogs
            .split(Named.as("enrichment-"))
            .branch((key, enriched) -> enriched.isFullyEnriched(), 
                Branched.as("complete"))
            .branch((key, enriched) -> true, 
                Branched.as("partial"))
            .noDefaultBranch();
        
        // Sink: fully enriched logs
        branches.get("enrichment-complete")
            .to("enriched-logs-complete", 
                Produced.with(Serdes.String(), enrichedLogEventSerde));
        
        // Sink: partially enriched logs (for retry or investigation)
        branches.get("enrichment-partial")
            .to("enriched-logs-partial",
                Produced.with(Serdes.String(), enrichedLogEventSerde));
        
        // Return the complete stream for testing
        return enrichedLogs;
    }
}
