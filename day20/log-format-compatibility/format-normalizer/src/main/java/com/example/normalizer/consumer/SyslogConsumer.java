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
public class SyslogConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(SyslogConsumer.class);
    
    private final ObjectMapper objectMapper;
    private final NormalizationService normalizationService;

    public SyslogConsumer(ObjectMapper objectMapper, 
                         NormalizationService normalizationService) {
        this.objectMapper = objectMapper;
        this.normalizationService = normalizationService;
    }

    @KafkaListener(topics = "raw-syslog-logs", groupId = "normalizer-group")
    public void consume(String message) {
        try {
            JsonNode syslogData = objectMapper.readTree(message);
            NormalizedLogEvent normalized = normalizeSyslog(syslogData);
            normalizationService.processNormalizedEvent(normalized);
        } catch (Exception e) {
            logger.error("Error normalizing syslog message", e);
        }
    }

    private NormalizedLogEvent normalizeSyslog(JsonNode syslogData) {
        NormalizedLogEvent event = new NormalizedLogEvent();
        event.setId(java.util.UUID.randomUUID().toString());
        event.setSource("syslog");
        
        // Normalize timestamp
        if (syslogData.has("timestamp")) {
            event.setTimestamp(java.time.Instant.parse(syslogData.get("timestamp").asText()));
        }
        
        // Normalize severity to level
        String severity = syslogData.has("severity") ? 
            syslogData.get("severity").asText() : "INFO";
        event.setLevel(mapSeverityToLevel(severity));
        
        // Extract common fields
        event.setHostname(syslogData.has("hostname") ? 
            syslogData.get("hostname").asText() : null);
        event.setApplication(syslogData.has("appName") ? 
            syslogData.get("appName").asText() : null);
        event.setMessage(syslogData.has("message") ? 
            syslogData.get("message").asText() : null);
        
        // Store original syslog-specific data
        Map<String, Object> rawFormat = new HashMap<>();
        rawFormat.put("facility", syslogData.has("facility") ? 
            syslogData.get("facility").asText() : null);
        rawFormat.put("protocol", syslogData.has("protocol") ? 
            syslogData.get("protocol").asText() : null);
        rawFormat.put("sourceIp", syslogData.has("sourceIp") ? 
            syslogData.get("sourceIp").asText() : null);
        event.setRawFormat(rawFormat);
        
        return event;
    }

    private String mapSeverityToLevel(String severity) {
        return switch (severity.toUpperCase()) {
            case "EMERGENCY", "ALERT", "CRITICAL" -> "CRITICAL";
            case "ERROR" -> "ERROR";
            case "WARNING", "NOTICE" -> "WARN";
            case "INFO" -> "INFO";
            case "DEBUG" -> "DEBUG";
            default -> "INFO";
        };
    }
}
