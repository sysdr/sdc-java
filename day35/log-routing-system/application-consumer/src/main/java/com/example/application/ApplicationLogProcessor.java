package com.example.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApplicationLogProcessor {
    
    private final Counter appLogsProcessed;
    
    public ApplicationLogProcessor(MeterRegistry meterRegistry) {
        this.appLogsProcessed = Counter.builder("application.logs.processed")
            .description("Number of application logs processed")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "logs-application", groupId = "application-consumer-group")
    public void processApplicationLog(LogEvent event) {
        try {
            log.info("Application log [{}] from {}: {}",
                event.getSeverity(), event.getSource(), event.getMessage());
            
            // Simulate error tracking integration
            if ("ERROR".equals(event.getSeverity()) || "FATAL".equals(event.getSeverity())) {
                trackError(event);
            }
            
            appLogsProcessed.increment();
        } catch (Exception e) {
            log.error("Error processing application log", e);
        }
    }
    
    private void trackError(LogEvent event) {
        // Simulate sending to error tracking service (e.g., Sentry, Rollbar)
        log.warn("Error tracked: {} - {}", event.getSource(), event.getMessage());
        
        // Could create ticket in JIRA
        if ("FATAL".equals(event.getSeverity())) {
            log.error("FATAL error - creating incident ticket for: {}", event.getId());
        }
    }
}
