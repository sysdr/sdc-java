package com.example.query.service;

import com.example.query.model.UserContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Role-Based Access Control for encrypted fields.
 * Defines which roles can decrypt which PII fields.
 */
@Service
public class AccessControlService {
    
    private static final Map<String, Set<String>> ROLE_FIELD_PERMISSIONS = Map.of(
        "ADMIN", Set.of("user.email", "user.name", "user.phone", "user.ssn", "payment.cardNumber"),
        "SUPPORT", Set.of("user.email", "user.name", "user.phone"),
        "ANALYST", Set.of("user.email", "user.name"),
        "COMPLIANCE", Set.of("user.email", "user.name", "user.ssn", "payment.cardNumber")
    );
    
    public Set<String> getAllowedFields(String role) {
        return ROLE_FIELD_PERMISSIONS.getOrDefault(role, Set.of());
    }
    
    public boolean canDecryptField(UserContext user, String fieldName) {
        Set<String> allowedFields = getAllowedFields(user.getRole());
        return allowedFields.contains(fieldName);
    }
}
