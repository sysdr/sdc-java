package com.example.system;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SystemLogProcessor {
    
    private final Counter systemLogsProcessed;
    
    public SystemLogProcessor(MeterRegistry meterRegistry) {
        this.systemLogsProcessed = Counter.builder("system.logs.processed")
            .description("Number of system logs processed")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = {"logs-system", "logs-default"}, groupId = "system-consumer-group")
    public void processSystemLog(LogEvent event) {
        try {
            log.info("System/Audit log from {}: {}", event.getSource(), event.getMessage());
            
            // Simulate archival to long-term storage
            archiveLog(event);
            
            systemLogsProcessed.increment();
        } catch (Exception e) {
            log.error("Error processing system log", e);
        }
    }
    
    private void archiveLog(LogEvent event) {
        // Simulate writing to S3/GCS for compliance
        log.debug("Archiving log to long-term storage: {}", event.getId());
        
        // For audit logs, ensure immutable storage
        if ("audit".equals(event.getType())) {
            log.info("Audit log archived with compliance metadata: {}", event.getId());
        }
    }
}
