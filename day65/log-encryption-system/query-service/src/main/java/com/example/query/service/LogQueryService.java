package com.example.query.service;

import com.example.query.entity.StoredLogEvent;
import com.example.query.model.EncryptedField;
import com.example.query.model.UserContext;
import com.example.query.repository.LogEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Query service with role-based field-level decryption.
 * Decrypts only fields that the user's role permits.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogQueryService {
    
    private final LogEventRepository logEventRepository;
    private final AccessControlService accessControlService;
    private final DecryptionClient decryptionClient;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    
    public List<Map<String, Object>> queryLogs(String eventType, UserContext user) {
        List<StoredLogEvent> events = eventType != null 
            ? logEventRepository.findByEventType(eventType)
            : logEventRepository.findAll();
        
        return events.stream()
            .map(event -> decryptAllowedFields(event, user))
            .collect(Collectors.toList());
    }
    
    public Map<String, Object> getLogById(String eventId, UserContext user) {
        StoredLogEvent event = logEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        
        return decryptAllowedFields(event, user);
    }
    
    private Map<String, Object> decryptAllowedFields(StoredLogEvent event, UserContext user) {
        Map<String, Object> result = new HashMap<>();
        result.put("eventId", event.getEventId());
        result.put("eventType", event.getEventType());
        result.put("timestamp", event.getTimestamp());
        result.put("severity", event.getSeverity());
        
        try {
            // Add public fields as-is
            Map<String, String> publicFields = objectMapper.readValue(
                event.getPublicFields(), 
                new TypeReference<Map<String, String>>() {}
            );
            result.put("publicFields", publicFields);
            
            // Process encrypted fields based on user role
            if (event.getEncryptedFields() != null) {
                List<EncryptedField> encryptedFields = objectMapper.readValue(
                    event.getEncryptedFields(),
                    new TypeReference<List<EncryptedField>>() {}
                );
                
                Map<String, String> decryptedPiiFields = new HashMap<>();
                
                for (EncryptedField encryptedField : encryptedFields) {
                    if (accessControlService.canDecryptField(user, encryptedField.getFieldName())) {
                        // User has permission - decrypt field
                        String decrypted = decryptionClient.decryptField(encryptedField);
                        decryptedPiiFields.put(encryptedField.getFieldName(), decrypted);
                        auditService.logDecryption(user, encryptedField.getFieldName(), event.getEventId());
                    } else {
                        // User lacks permission - redact field
                        decryptedPiiFields.put(encryptedField.getFieldName(), "[REDACTED]");
                        auditService.logAccessDenied(user, encryptedField.getFieldName(), event.getEventId());
                    }
                }
                
                result.put("piiFields", decryptedPiiFields);
            }
            
        } catch (Exception e) {
            log.error("Failed to process fields for event: {}", event.getEventId(), e);
        }
        
        return result;
    }
}
