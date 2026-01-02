package com.example.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SecurityLogProcessor {
    
    private final Counter securityEventsProcessed;
    
    public SecurityLogProcessor(MeterRegistry meterRegistry) {
        this.securityEventsProcessed = Counter.builder("security.events.processed")
            .description("Number of security events processed")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "logs-security", groupId = "security-consumer-group")
    public void processSecurityLog(LogEvent event) {
        try {
            log.warn("SECURITY EVENT: [{}] {} from {} - {}",
                event.getSeverity(), event.getId(), event.getSource(), event.getMessage());
            
            // Simulate security analysis
            analyzeSecurityEvent(event);
            
            // Simulate alerting
            if ("FATAL".equals(event.getSeverity()) || "ERROR".equals(event.getSeverity())) {
                triggerAlert(event);
            }
            
            securityEventsProcessed.increment();
        } catch (Exception e) {
            log.error("Error processing security log", e);
        }
    }
    
    @KafkaListener(topics = "logs-critical", groupId = "critical-consumer-group")
    public void processCriticalLog(LogEvent event) {
        try {
            log.error("CRITICAL EVENT: [{}] {} - {}",
                event.getSeverity(), event.getId(), event.getMessage());
            
            // Immediate escalation for critical events
            triggerAlert(event);
            
        } catch (Exception e) {
            log.error("Error processing critical log", e);
        }
    }
    
    private void analyzeSecurityEvent(LogEvent event) {
        // Simulate threat intelligence check
        log.debug("Analyzing security event: {}", event.getId());
        
        // Check for known patterns
        if (event.getMessage().contains("failed login") || 
            event.getMessage().contains("unauthorized")) {
            log.warn("Potential security threat detected: {}", event.getId());
        }
    }
    
    private void triggerAlert(LogEvent event) {
        // Simulate PagerDuty/Slack alert
        log.error("ALERT TRIGGERED: {} - {} [{}]",
            event.getSource(), event.getMessage(), event.getSeverity());
    }
}
