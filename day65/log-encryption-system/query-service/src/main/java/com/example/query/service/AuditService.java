package com.example.query.service;

import com.example.query.model.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Audit logging for PII field decryption.
 * Records who decrypted what, when for compliance.
 */
@Service
@Slf4j
public class AuditService {
    
    public void logDecryption(UserContext user, String fieldName, String eventId) {
        // In production, this would write to a secure audit log database
        log.info("AUDIT: User {} (role: {}) decrypted field '{}' from event {} at {}", 
            user.getUserId(), 
            user.getRole(), 
            fieldName, 
            eventId, 
            Instant.now());
    }
    
    public void logAccessDenied(UserContext user, String fieldName, String eventId) {
        log.warn("AUDIT: Access DENIED - User {} (role: {}) attempted to decrypt field '{}' from event {}", 
            user.getUserId(), 
            user.getRole(), 
            fieldName, 
            eventId);
    }
}
