package com.example.logprocessor.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class SchemaValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaValidationService.class);
    
    private final ObjectMapper objectMapper;
    private JsonSchema cachedSchema;
    
    @Value("classpath:schemas/log-event-schema-v1.json")
    private Resource schemaResource;
    
    public SchemaValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void init() throws IOException {
        // Pre-load schema at startup for fast validation
        cachedSchema = loadSchemaFromResource();
        logger.info("Schema validation service initialized with cached schema");
    }
    
    /**
     * Validate log event against JSON Schema
     * Returns true if valid, throws exception with details if invalid
     */
    @CircuitBreaker(name = "schemaValidation", fallbackMethod = "validationFallback")
    public ValidationResult validate(LogEvent event) {
        try {
            // Convert LogEvent to JsonNode
            JsonNode jsonNode = objectMapper.valueToTree(event);
            
            // Perform schema validation
            Set<ValidationMessage> errors = cachedSchema.validate(jsonNode);
            
            if (errors.isEmpty()) {
                return new ValidationResult(true, null);
            } else {
                String errorMessage = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown validation error");
                
                logger.warn("Schema validation failed for event: {}", errorMessage);
                return new ValidationResult(false, errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error during schema validation", e);
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Fallback method for circuit breaker
     */
    public ValidationResult validationFallback(LogEvent event, Exception e) {
        logger.error("Schema validation circuit breaker activated", e);
        // In fallback, we accept the event but log the issue
        return new ValidationResult(true, "Validation bypassed due to circuit breaker");
    }
    
    @Cacheable("schemas")
    private JsonSchema loadSchemaFromResource() throws IOException {
        try (InputStream schemaStream = schemaResource.getInputStream()) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonNode schemaNode = objectMapper.readTree(schemaStream);
            return factory.getSchema(schemaNode);
        }
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
