package com.example.schemaregistry.service;

import com.example.schemaregistry.entity.SchemaType;
import com.example.schemaregistry.exception.InvalidSchemaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SchemaValidatorService {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaValidatorService.class);
    private final ObjectMapper objectMapper;
    
    public SchemaValidatorService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }
    
    public void validateSyntax(String schema, SchemaType schemaType) {
        try {
            switch (schemaType) {
                case AVRO -> validateAvroSyntax(schema);
                case JSON -> validateJsonSchemaSyntax(schema);
                case PROTOBUF -> validateProtobufSyntax(schema);
            }
        } catch (InvalidSchemaException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSchemaException("Invalid " + schemaType + " schema: " + e.getMessage(), e);
        }
    }
    
    public String normalize(String schema, SchemaType schemaType) {
        try {
            return switch (schemaType) {
                case AVRO -> normalizeAvro(schema);
                case JSON -> normalizeJson(schema);
                case PROTOBUF -> schema.trim(); // Protobuf normalization is complex
            };
        } catch (Exception e) {
            log.warn("Failed to normalize schema, using original", e);
            return schema;
        }
    }
    
    private void validateAvroSyntax(String schema) {
        try {
            new Schema.Parser().parse(schema);
        } catch (Exception e) {
            throw new InvalidSchemaException("Invalid Avro schema: " + e.getMessage());
        }
    }
    
    private void validateJsonSchemaSyntax(String schema) {
        try {
            JsonNode node = objectMapper.readTree(schema);
            if (!node.isObject()) {
                throw new InvalidSchemaException("JSON Schema must be an object");
            }
        } catch (InvalidSchemaException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSchemaException("Invalid JSON Schema: " + e.getMessage());
        }
    }
    
    private void validateProtobufSyntax(String schema) {
        // Basic syntax check - ensure it contains message definition
        if (!schema.contains("message")) {
            throw new InvalidSchemaException("Invalid Protobuf schema: no message definition found");
        }
    }
    
    private String normalizeAvro(String schema) throws Exception {
        Schema parsed = new Schema.Parser().parse(schema);
        return parsed.toString();
    }
    
    private String normalizeJson(String schema) throws Exception {
        JsonNode node = objectMapper.readTree(schema);
        return objectMapper.writeValueAsString(node);
    }
}
