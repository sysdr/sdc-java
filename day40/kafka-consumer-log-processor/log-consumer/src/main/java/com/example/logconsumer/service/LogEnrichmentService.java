package com.example.logconsumer.service;

import com.example.logconsumer.model.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class LogEnrichmentService {

    private static final Map<String, String> SEVERITY_MAPPING = new HashMap<>();
    
    static {
        SEVERITY_MAPPING.put("ERROR", "HIGH");
        SEVERITY_MAPPING.put("WARN", "MEDIUM");
        SEVERITY_MAPPING.put("INFO", "LOW");
        SEVERITY_MAPPING.put("DEBUG", "LOW");
    }

    public LogEvent enrich(LogEvent logEvent) {
        // Add severity based on level
        logEvent.setSeverity(SEVERITY_MAPPING.getOrDefault(logEvent.getLevel(), "UNKNOWN"));
        
        // Add processing metadata
        logEvent.setProcessedAt(Instant.now());
        logEvent.setCreatedAt(Instant.now());
        
        // Calculate processing time (simulated)
        if (logEvent.getTimestamp() != null) {
            long processingTimeMs = Instant.now().toEpochMilli() - 
                                   logEvent.getTimestamp().toEpochMilli();
            logEvent.setProcessingTime(processingTimeMs);
        }
        
        return logEvent;
    }
}
