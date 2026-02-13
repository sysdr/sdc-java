package com.example.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AuditConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditConsumer(AuditLogRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @KafkaListener(topics = "audit-trail", groupId = "audit-service")
    public void consume(String message) {
        try {
            AuditLog auditLog = objectMapper.readValue(message, AuditLog.class);
            repository.save(auditLog);
            log.info("Saved audit log: {} by user {}", auditLog.getAction(), auditLog.getUsername());
        } catch (Exception e) {
            log.error("Failed to process audit event", e);
        }
    }
}
