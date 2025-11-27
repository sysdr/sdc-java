package com.example.normalizer.service;

import com.example.normalizer.model.NormalizedLogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NormalizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NormalizationService.class);
    private static final String NORMALIZED_TOPIC = "normalized-logs";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter normalizedEvents;
    private final Counter normalizationErrors;

    public NormalizationService(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.normalizedEvents = Counter.builder("normalizer.events.processed")
                .description("Total normalized events")
                .register(meterRegistry);
        this.normalizationErrors = Counter.builder("normalizer.errors")
                .description("Normalization errors")
                .register(meterRegistry);
    }

    public void processNormalizedEvent(NormalizedLogEvent event) {
        try {
            // Validate event
            if (!isValid(event)) {
                logger.warn("Invalid normalized event: {}", event.getId());
                normalizationErrors.increment();
                return;
            }

            // Send to normalized topic
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(NORMALIZED_TOPIC, event.getHostname(), json);
            normalizedEvents.increment();
            
            logger.debug("Normalized event from {} source: {}", event.getSource(), event.getId());
        } catch (Exception e) {
            normalizationErrors.increment();
            logger.error("Error processing normalized event", e);
        }
    }

    private boolean isValid(NormalizedLogEvent event) {
        return event.getTimestamp() != null 
            && event.getLevel() != null 
            && event.getSource() != null 
            && event.getMessage() != null;
    }
}
