package com.example.schemaregistry.service;

import com.example.schemaregistry.dto.CompatibilityCheckResponse;
import com.example.schemaregistry.entity.CompatibilityMode;
import com.example.schemaregistry.entity.SchemaType;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaCompatibility.SchemaCompatibilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompatibilityCheckerService {
    
    private static final Logger log = LoggerFactory.getLogger(CompatibilityCheckerService.class);
    
    public CompatibilityCheckResponse checkCompatibility(
            String newSchema,
            SchemaType schemaType,
            List<String> previousSchemas,
            CompatibilityMode mode) {
        
        if (previousSchemas.isEmpty() || mode == CompatibilityMode.NONE) {
            return CompatibilityCheckResponse.compatible();
        }
        
        return switch (schemaType) {
            case AVRO -> checkAvroCompatibility(newSchema, previousSchemas, mode);
            case JSON -> checkJsonSchemaCompatibility(newSchema, previousSchemas, mode);
            case PROTOBUF -> checkProtobufCompatibility(newSchema, previousSchemas, mode);
        };
    }
    
    private CompatibilityCheckResponse checkAvroCompatibility(
            String newSchemaStr,
            List<String> previousSchemas,
            CompatibilityMode mode) {
        
        try {
            Schema proposed = new Schema.Parser().parse(newSchemaStr);
            
            for (String oldSchemaStr : previousSchemas) {
                Schema existing = new Schema.Parser().parse(oldSchemaStr);
                
                // Check backward compatibility (new can read old)
                if (mode == CompatibilityMode.BACKWARD || mode == CompatibilityMode.FULL) {
                    SchemaCompatibility.SchemaPairCompatibility result = 
                        SchemaCompatibility.checkReaderWriterCompatibility(proposed, existing);
                    
                    if (result.getType() != SchemaCompatibilityType.COMPATIBLE) {
                        return CompatibilityCheckResponse.incompatible(
                            "BACKWARD incompatibility: New schema cannot read old data. " + 
                            result.getDescription());
                    }
                }
                
                // Check forward compatibility (old can read new)
                if (mode == CompatibilityMode.FORWARD || mode == CompatibilityMode.FULL) {
                    SchemaCompatibility.SchemaPairCompatibility result = 
                        SchemaCompatibility.checkReaderWriterCompatibility(existing, proposed);
                    
                    if (result.getType() != SchemaCompatibilityType.COMPATIBLE) {
                        return CompatibilityCheckResponse.incompatible(
                            "FORWARD incompatibility: Old schema cannot read new data. " + 
                            result.getDescription());
                    }
                }
            }
            
            return CompatibilityCheckResponse.compatible();
            
        } catch (Exception e) {
            log.error("Error checking Avro compatibility", e);
            return CompatibilityCheckResponse.incompatible("Error parsing schema: " + e.getMessage());
        }
    }
    
    private CompatibilityCheckResponse checkJsonSchemaCompatibility(
            String newSchema,
            List<String> previousSchemas,
            CompatibilityMode mode) {
        // Simplified JSON Schema compatibility check
        // In production, use a proper JSON Schema compatibility library
        log.warn("JSON Schema compatibility checking is simplified");
        return CompatibilityCheckResponse.compatible();
    }
    
    private CompatibilityCheckResponse checkProtobufCompatibility(
            String newSchema,
            List<String> previousSchemas,
            CompatibilityMode mode) {
        // Simplified Protobuf compatibility check
        // In production, use protobuf-specific compatibility rules
        log.warn("Protobuf compatibility checking is simplified");
        return CompatibilityCheckResponse.compatible();
    }
}
