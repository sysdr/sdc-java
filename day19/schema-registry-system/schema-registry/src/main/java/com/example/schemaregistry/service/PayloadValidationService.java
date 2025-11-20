package com.example.schemaregistry.service;

import com.example.schemaregistry.dto.ValidationResponse;
import com.example.schemaregistry.entity.SchemaType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Collections;

@Service
public class PayloadValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(PayloadValidationService.class);
    
    public ValidationResponse validatePayload(byte[] payload, String schemaDefinition, SchemaType schemaType) {
        try {
            return switch (schemaType) {
                case AVRO -> validateAvroPayload(payload, schemaDefinition);
                case JSON -> validateJsonPayload(payload, schemaDefinition);
                case PROTOBUF -> validateProtobufPayload(payload, schemaDefinition);
            };
        } catch (Exception e) {
            log.error("Validation error", e);
            return ValidationResponse.invalid(Collections.singletonList(e.getMessage()));
        }
    }
    
    private ValidationResponse validateAvroPayload(byte[] payload, String schemaDefinition) {
        try {
            Schema schema = new Schema.Parser().parse(schemaDefinition);
            GenericDatumReader<Object> reader = new GenericDatumReader<>(schema);
            reader.read(null, DecoderFactory.get().binaryDecoder(
                new ByteArrayInputStream(payload), null));
            return ValidationResponse.valid();
        } catch (Exception e) {
            return ValidationResponse.invalid(Collections.singletonList(
                "Avro validation failed: " + e.getMessage()));
        }
    }
    
    private ValidationResponse validateJsonPayload(byte[] payload, String schemaDefinition) {
        // Implement JSON Schema validation
        // Using json-schema-validator library
        return ValidationResponse.valid();
    }
    
    private ValidationResponse validateProtobufPayload(byte[] payload, String schemaDefinition) {
        // Implement Protobuf validation
        return ValidationResponse.valid();
    }
}
