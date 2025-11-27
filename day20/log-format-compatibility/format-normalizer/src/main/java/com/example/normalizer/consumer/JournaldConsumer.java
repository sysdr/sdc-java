package com.example.normalizer.consumer;

import com.example.normalizer.model.NormalizedLogEvent;
import com.example.normalizer.service.NormalizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JournaldConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(JournaldConsumer.class);
    
    private final ObjectMapper objectMapper;
    private final NormalizationService normalizationService;

    public JournaldConsumer(ObjectMapper objectMapper, 
                           NormalizationService normalizationService) {
        this.objectMapper = objectMapper;
        this.normalizationService = normalizationService;
    }

    @KafkaListener(topics = "raw-journald-logs", groupId = "normalizer-group")
    public void consume(String message) {
        try {
            JsonNode journaldData = objectMapper.readTree(message);
            NormalizedLogEvent normalized = normalizeJournald(journaldData);
            normalizationService.processNormalizedEvent(normalized);
        } catch (Exception e) {
            logger.error("Error normalizing journald message", e);
        }
    }

    private NormalizedLogEvent normalizeJournald(JsonNode journaldData) {
        NormalizedLogEvent event = new NormalizedLogEvent();
        event.setId(java.util.UUID.randomUUID().toString());
        event.setSource("journald");
        
        // Normalize timestamp
        if (journaldData.has("timestamp")) {
            event.setTimestamp(java.time.Instant.parse(journaldData.get("timestamp").asText()));
        }
        
        // Normalize priority to level
        String priority = journaldData.has("priority") ? 
            journaldData.get("priority").asText() : "INFO";
        event.setLevel(mapPriorityToLevel(priority));
        
        // Extract common fields
        event.setHostname(journaldData.has("hostname") ? 
            journaldData.get("hostname").asText() : null);
        event.setApplication(journaldData.has("unit") ? 
            journaldData.get("unit").asText() : null);
        event.setMessage(journaldData.has("message") ? 
            journaldData.get("message").asText() : null);
        
        // Store original journald-specific data
        Map<String, Object> rawFormat = new HashMap<>();
        rawFormat.put("pid", journaldData.has("pid") ? 
            journaldData.get("pid").asText() : null);
        rawFormat.put("uid", journaldData.has("uid") ? 
            journaldData.get("uid").asText() : null);
        rawFormat.put("cgroup", journaldData.has("cgroup") ? 
            journaldData.get("cgroup").asText() : null);
        rawFormat.put("containerId", journaldData.has("containerId") ? 
            journaldData.get("containerId").asText() : null);
        event.setRawFormat(rawFormat);
        
        return event;
    }

    private String mapPriorityToLevel(String priority) {
        return switch (priority.toUpperCase()) {
            case "0", "1", "2" -> "CRITICAL";
            case "3" -> "ERROR";
            case "4" -> "WARN";
            case "5", "6" -> "INFO";
            case "7" -> "DEBUG";
            default -> "INFO";
        };
    }
}
