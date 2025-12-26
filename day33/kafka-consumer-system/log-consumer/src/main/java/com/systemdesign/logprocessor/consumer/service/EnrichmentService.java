package com.systemdesign.logprocessor.consumer.service;

import com.systemdesign.logprocessor.model.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EnrichmentService {

    /**
     * Enrich log events with additional context from external systems.
     * In production, this might query user databases, configuration services, etc.
     */
    public LogEvent enrich(LogEvent event) {
        // Simulate enrichment with application metadata
        Map<String, String> enrichedMetadata = new HashMap<>();
        
        if (event.getMetadata() != null) {
            enrichedMetadata.putAll(event.getMetadata());
        }
        
        // Add environment information
        enrichedMetadata.put("environment", "production");
        enrichedMetadata.put("region", "us-east-1");
        enrichedMetadata.put("enriched_at", String.valueOf(System.currentTimeMillis()));
        
        // Add severity classification based on log level
        String severity = classifySeverity(event.getLevel());
        enrichedMetadata.put("severity", severity);
        
        event.setMetadata(enrichedMetadata);
        event.setEnrichedData(buildEnrichedSummary(event));
        event.setProcessingTimestamp(System.currentTimeMillis());
        
        return event;
    }

    private String classifySeverity(String level) {
        return switch (level.toUpperCase()) {
            case "ERROR", "FATAL" -> "CRITICAL";
            case "WARN" -> "WARNING";
            case "INFO" -> "NORMAL";
            default -> "LOW";
        };
    }

    private String buildEnrichedSummary(LogEvent event) {
        return String.format("[%s] %s on %s - %s", 
            event.getLevel(), 
            event.getService(), 
            event.getHost(),
            event.getMessage());
    }
}
