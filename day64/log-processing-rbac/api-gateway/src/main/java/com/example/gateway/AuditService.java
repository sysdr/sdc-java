package com.example.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String AUDIT_TOPIC = "audit-trail";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AuditService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public void logAccess(UserContext user, String action, String resource, 
                         String result, String query, Integer recordsReturned) {
        try {
            AuditEvent event = new AuditEvent();
            event.setUserId(user.getUserId());
            event.setUsername(user.getUsername());
            event.setAction(action);
            event.setResource(resource);
            event.setResult(result);
            event.setQuery(query);
            event.setRecordsReturned(recordsReturned);

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(AUDIT_TOPIC, user.getUserId().toString(), json);
            
            log.debug("Audit event logged: {} by {}", action, user.getUsername());
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }
}
