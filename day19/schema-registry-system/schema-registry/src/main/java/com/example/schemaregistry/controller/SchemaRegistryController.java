package com.example.schemaregistry.controller;

import com.example.schemaregistry.dto.*;
import com.example.schemaregistry.entity.CompatibilityMode;
import com.example.schemaregistry.service.PayloadValidationService;
import com.example.schemaregistry.service.SchemaRegistryService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects")
public class SchemaRegistryController {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryController.class);
    
    private final SchemaRegistryService registryService;
    private final PayloadValidationService validationService;
    
    public SchemaRegistryController(
            SchemaRegistryService registryService,
            PayloadValidationService validationService) {
        this.registryService = registryService;
        this.validationService = validationService;
    }
    
    @GetMapping
    public ResponseEntity<List<String>> getAllSubjects() {
        return ResponseEntity.ok(registryService.getAllSubjects());
    }
    
    @PostMapping("/{subject}/versions")
    @CircuitBreaker(name = "schemaRegistry", fallbackMethod = "registerSchemaFallback")
    public ResponseEntity<RegisterSchemaResponse> registerSchema(
            @PathVariable String subject,
            @Valid @RequestBody RegisterSchemaRequest request) {
        RegisterSchemaResponse response = registryService.registerSchema(subject, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{subject}/versions")
    public ResponseEntity<List<Integer>> getVersions(@PathVariable String subject) {
        return ResponseEntity.ok(registryService.getVersions(subject));
    }
    
    @GetMapping("/{subject}/versions/{version}")
    public ResponseEntity<SchemaResponse> getSchema(
            @PathVariable String subject,
            @PathVariable String version) {
        
        SchemaResponse response;
        if ("latest".equalsIgnoreCase(version)) {
            response = registryService.getLatestSchema(subject);
        } else {
            response = registryService.getSchema(subject, Integer.parseInt(version));
        }
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{subject}")
    public ResponseEntity<Void> deleteSubject(@PathVariable String subject) {
        registryService.deleteSubject(subject);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{subject}/compatibility")
    public ResponseEntity<CompatibilityCheckResponse> testCompatibility(
            @PathVariable String subject,
            @Valid @RequestBody CompatibilityCheckRequest request) {
        return ResponseEntity.ok(registryService.testCompatibility(subject, request));
    }
    
    @GetMapping("/{subject}/config")
    public ResponseEntity<ConfigResponse> getConfig(@PathVariable String subject) {
        CompatibilityMode mode = registryService.getCompatibilityMode(subject);
        return ResponseEntity.ok(new ConfigResponse(mode));
    }
    
    @PutMapping("/{subject}/config")
    public ResponseEntity<ConfigResponse> updateConfig(
            @PathVariable String subject,
            @Valid @RequestBody ConfigUpdateRequest request) {
        registryService.setCompatibilityMode(subject, request.getCompatibility());
        return ResponseEntity.ok(new ConfigResponse(request.getCompatibility()));
    }
    
    @PostMapping("/{subject}/versions/{version}/validate")
    public ResponseEntity<ValidationResponse> validatePayload(
            @PathVariable String subject,
            @PathVariable Integer version,
            @RequestBody byte[] payload) {
        SchemaResponse schema = registryService.getSchema(subject, version);
        ValidationResponse response = validationService.validatePayload(
            payload, schema.getSchema(), schema.getSchemaType());
        return ResponseEntity.ok(response);
    }
    
    // Fallback method for circuit breaker
    public ResponseEntity<RegisterSchemaResponse> registerSchemaFallback(
            String subject, RegisterSchemaRequest request, Throwable t) {
        log.error("Circuit breaker triggered for schema registration: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
