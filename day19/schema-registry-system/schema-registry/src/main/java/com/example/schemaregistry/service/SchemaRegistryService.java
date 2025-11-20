package com.example.schemaregistry.service;

import com.example.schemaregistry.dto.*;
import com.example.schemaregistry.entity.*;
import com.example.schemaregistry.exception.*;
import com.example.schemaregistry.repository.*;
import com.google.common.hash.Hashing;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchemaRegistryService {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryService.class);
    
    private final SchemaRepository schemaRepository;
    private final SubjectConfigRepository configRepository;
    private final CompatibilityCheckerService compatibilityChecker;
    private final SchemaValidatorService schemaValidator;
    
    private final Counter registrationCounter;
    private final Counter compatibilityFailureCounter;
    private final Timer registrationTimer;
    
    public SchemaRegistryService(
            SchemaRepository schemaRepository,
            SubjectConfigRepository configRepository,
            CompatibilityCheckerService compatibilityChecker,
            SchemaValidatorService schemaValidator,
            MeterRegistry meterRegistry) {
        this.schemaRepository = schemaRepository;
        this.configRepository = configRepository;
        this.compatibilityChecker = compatibilityChecker;
        this.schemaValidator = schemaValidator;
        
        this.registrationCounter = meterRegistry.counter("schema.registrations");
        this.compatibilityFailureCounter = meterRegistry.counter("schema.compatibility.failures");
        this.registrationTimer = meterRegistry.timer("schema.registration.time");
    }
    
    @Transactional
    @CacheEvict(value = {"schemas", "schema-ids", "latest-versions", "subject-schemas"}, allEntries = true)
    public RegisterSchemaResponse registerSchema(String subject, RegisterSchemaRequest request) {
        return registrationTimer.record(() -> {
            log.info("Registering schema for subject: {}", subject);
            
            // Validate schema syntax
            schemaValidator.validateSyntax(request.getSchema(), request.getSchemaType());
            
            // Normalize and fingerprint
            String normalized = schemaValidator.normalize(request.getSchema(), request.getSchemaType());
            String fingerprint = computeFingerprint(normalized);
            
            // Check if identical schema already exists for this subject
            if (schemaRepository.existsBySubjectAndFingerprint(subject, fingerprint)) {
                SchemaEntity existing = schemaRepository.findAllBySubjectOrderByVersionDesc(subject).stream()
                    .filter(s -> s.getFingerprint().equals(fingerprint))
                    .findFirst()
                    .orElseThrow();
                log.info("Schema already exists for subject {} with ID {}", subject, existing.getId());
                return new RegisterSchemaResponse(existing.getId());
            }
            
            // Get existing schemas for compatibility check
            List<SchemaEntity> existingSchemas = schemaRepository.findAllBySubjectOrderByVersionDesc(subject);
            
            // Check compatibility
            if (!existingSchemas.isEmpty()) {
                CompatibilityMode mode = getCompatibilityMode(subject);
                if (mode != CompatibilityMode.NONE) {
                    List<String> previousSchemas = existingSchemas.stream()
                        .map(SchemaEntity::getSchema)
                        .collect(Collectors.toList());
                    
                    CompatibilityCheckResponse result = compatibilityChecker.checkCompatibility(
                        request.getSchema(), 
                        request.getSchemaType(),
                        previousSchemas, 
                        mode
                    );
                    
                    if (!result.isCompatible()) {
                        compatibilityFailureCounter.increment();
                        throw new IncompatibleSchemaException(result.getMessage());
                    }
                }
            }
            
            // Determine next version
            Integer nextVersion = schemaRepository.findMaxVersionBySubject(subject) + 1;
            
            // Create and save entity
            SchemaEntity entity = new SchemaEntity();
            entity.setSubject(subject);
            entity.setVersion(nextVersion);
            entity.setSchemaType(request.getSchemaType());
            entity.setSchema(request.getSchema());
            entity.setFingerprint(fingerprint);
            
            SchemaEntity saved = schemaRepository.save(entity);
            registrationCounter.increment();
            
            log.info("Registered schema {} version {} with ID {}", subject, nextVersion, saved.getId());
            return new RegisterSchemaResponse(saved.getId());
        });
    }
    
    @Cacheable(value = "schemas", key = "#id")
    public SchemaResponse getSchemaById(Long id) {
        log.debug("Fetching schema by ID: {}", id);
        return schemaRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new SchemaNotFoundException(id));
    }
    
    @Cacheable(value = "schema-ids", key = "#subject + '-' + #version")
    public SchemaResponse getSchema(String subject, Integer version) {
        log.debug("Fetching schema: {} version {}", subject, version);
        return schemaRepository.findBySubjectAndVersion(subject, version)
            .map(this::toResponse)
            .orElseThrow(() -> new SchemaNotFoundException(subject, version));
    }
    
    @Cacheable(value = "latest-versions", key = "#subject")
    public SchemaResponse getLatestSchema(String subject) {
        log.debug("Fetching latest schema for subject: {}", subject);
        return schemaRepository.findLatestBySubject(subject)
            .map(this::toResponse)
            .orElseThrow(() -> new SchemaNotFoundException("No schemas found for subject: " + subject));
    }
    
    public List<String> getAllSubjects() {
        return schemaRepository.findAllSubjects();
    }
    
    public List<Integer> getVersions(String subject) {
        return schemaRepository.findVersionsBySubject(subject);
    }
    
    @Cacheable(value = "subject-schemas", key = "#subject")
    public List<SchemaResponse> getAllSchemasBySubject(String subject) {
        return schemaRepository.findAllBySubjectOrderByVersionDesc(subject).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    public CompatibilityMode getCompatibilityMode(String subject) {
        return configRepository.findById(subject)
            .map(SubjectConfig::getCompatibilityMode)
            .orElse(CompatibilityMode.BACKWARD); // Default
    }
    
    @Transactional
    public void setCompatibilityMode(String subject, CompatibilityMode mode) {
        SubjectConfig config = configRepository.findById(subject)
            .orElseGet(() -> {
                SubjectConfig newConfig = new SubjectConfig();
                newConfig.setSubject(subject);
                return newConfig;
            });
        config.setCompatibilityMode(mode);
        configRepository.save(config);
        log.info("Set compatibility mode for {} to {}", subject, mode);
    }
    
    public CompatibilityCheckResponse testCompatibility(String subject, CompatibilityCheckRequest request) {
        List<SchemaEntity> existingSchemas = schemaRepository.findAllBySubjectOrderByVersionDesc(subject);
        
        if (existingSchemas.isEmpty()) {
            return CompatibilityCheckResponse.compatible();
        }
        
        CompatibilityMode mode = getCompatibilityMode(subject);
        if (mode == CompatibilityMode.NONE) {
            return CompatibilityCheckResponse.compatible();
        }
        
        List<String> previousSchemas = existingSchemas.stream()
            .map(SchemaEntity::getSchema)
            .collect(Collectors.toList());
        
        return compatibilityChecker.checkCompatibility(
            request.getSchema(),
            request.getSchemaType(),
            previousSchemas,
            mode
        );
    }
    
    @Transactional
    @CacheEvict(value = {"schemas", "schema-ids", "latest-versions", "subject-schemas"}, allEntries = true)
    public void deleteSubject(String subject) {
        log.warn("Deleting all schemas for subject: {}", subject);
        schemaRepository.deleteBySubject(subject);
        configRepository.deleteById(subject);
    }
    
    private String computeFingerprint(String schema) {
        return Hashing.sha256()
            .hashString(schema, StandardCharsets.UTF_8)
            .toString();
    }
    
    private SchemaResponse toResponse(SchemaEntity entity) {
        SchemaResponse response = new SchemaResponse();
        response.setId(entity.getId());
        response.setSubject(entity.getSubject());
        response.setVersion(entity.getVersion());
        response.setSchemaType(entity.getSchemaType());
        response.setSchema(entity.getSchema());
        return response;
    }
}
