package com.example.logprocessor.service;

import com.example.logprocessor.model.LogEvent;
import com.example.logprocessor.model.PriorityLevel;
import org.springframework.stereotype.Service;

@Service
public class PriorityClassifier {
    
    /**
     * Classify log event priority based on content analysis
     * 
     * CRITICAL: System failures, OOM errors, 5xx responses
     * HIGH: Client errors (4xx), slow queries, exceptions
     * NORMAL: ERROR level logs, warnings
     * LOW: INFO, DEBUG logs
     */
    public PriorityLevel classify(LogEvent event) {
        // Critical: Exceptions, OOM, 5xx errors
        if (event.containsException() && 
            (event.getException().contains("OutOfMemoryError") || 
             event.getException().contains("StackOverflowError"))) {
            return PriorityLevel.CRITICAL;
        }
        
        if (event.getHttpStatus() != null && event.getHttpStatus() >= 500) {
            return PriorityLevel.CRITICAL;
        }
        
        if (event.getMessage().toLowerCase().contains("database") && 
            event.getMessage().toLowerCase().contains("down")) {
            return PriorityLevel.CRITICAL;
        }
        
        // High: 4xx errors, slow queries, general exceptions
        if (event.getHttpStatus() != null && event.getHttpStatus() >= 400) {
            return PriorityLevel.HIGH;
        }
        
        if (event.getLatencyMs() != null && event.getLatencyMs() > 1000) {
            return PriorityLevel.HIGH;
        }
        
        if (event.containsException()) {
            return PriorityLevel.HIGH;
        }
        
        // Normal: ERROR level logs
        if ("ERROR".equals(event.getLevel())) {
            return PriorityLevel.NORMAL;
        }
        
        // Low: Everything else (INFO, DEBUG, WARN)
        return PriorityLevel.LOW;
    }
}
