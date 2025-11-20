package com.example.schemaclient.service;

import com.example.schemaclient.dto.SchemaMetadata;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchemaRegistryClient {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryClient.class);
    
    private final RestTemplate restTemplate;
    private final String registryUrl;
    
    // Local cache for compiled Avro schemas
    private final Map<Long, Schema> compiledSchemaCache = new ConcurrentHashMap<>();
    
    public SchemaRegistryClient(
            RestTemplate restTemplate,
            @Value("${schema.registry.url:http://localhost:8081}") String registryUrl) {
        this.restTemplate = restTemplate;
        this.registryUrl = registryUrl;
    }
    
    @Cacheable(value = "client-schemas", key = "#id")
    @CircuitBreaker(name = "schemaClient", fallbackMethod = "getSchemaFallback")
    @Retry(name = "schemaClient")
    public SchemaMetadata getSchemaById(Long id) {
        log.debug("Fetching schema ID {} from registry", id);
        String url = registryUrl + "/schemas/ids/" + id;
        ResponseEntity<SchemaMetadata> response = restTemplate.getForEntity(url, SchemaMetadata.class);
        return response.getBody();
    }
    
    @Cacheable(value = "client-schemas-by-subject", key = "#subject + '-' + #version")
    @CircuitBreaker(name = "schemaClient", fallbackMethod = "getSchemaBySubjectFallback")
    @Retry(name = "schemaClient")
    public SchemaMetadata getSchema(String subject, Integer version) {
        log.debug("Fetching schema {}-{} from registry", subject, version);
        String url = registryUrl + "/subjects/" + subject + "/versions/" + version;
        ResponseEntity<SchemaMetadata> response = restTemplate.getForEntity(url, SchemaMetadata.class);
        return response.getBody();
    }
    
    public Long registerSchema(String subject, String schema, String schemaType) {
        String url = registryUrl + "/subjects/" + subject + "/versions";
        Map<String, String> request = Map.of(
            "schema", schema,
            "schemaType", schemaType
        );
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        return ((Number) response.getBody().get("id")).longValue();
    }
    
    /**
     * Get compiled Avro schema - caches the compiled Schema object for performance
     */
    public Schema getCompiledAvroSchema(Long id) {
        return compiledSchemaCache.computeIfAbsent(id, schemaId -> {
            SchemaMetadata metadata = getSchemaById(schemaId);
            return new Schema.Parser().parse(metadata.getSchema());
        });
    }
    
    // Fallback methods
    private SchemaMetadata getSchemaFallback(Long id, Throwable t) {
        log.error("Failed to fetch schema {}: {}", id, t.getMessage());
        throw new RuntimeException("Schema registry unavailable", t);
    }
    
    private SchemaMetadata getSchemaBySubjectFallback(String subject, Integer version, Throwable t) {
        log.error("Failed to fetch schema {}-{}: {}", subject, version, t.getMessage());
        throw new RuntimeException("Schema registry unavailable", t);
    }
}
