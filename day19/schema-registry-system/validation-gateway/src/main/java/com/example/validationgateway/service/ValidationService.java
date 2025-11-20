package com.example.validationgateway.service;

import com.example.schemaclient.dto.SchemaMetadata;
import com.example.schemaclient.service.SchemaRegistryClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Service
public class ValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);
    private static final byte MAGIC_BYTE = 0x0;
    
    private final SchemaRegistryClient schemaClient;
    private final Counter validationSuccessCounter;
    private final Counter validationFailureCounter;
    private final Timer validationTimer;
    
    public ValidationService(SchemaRegistryClient schemaClient, MeterRegistry meterRegistry) {
        this.schemaClient = schemaClient;
        this.validationSuccessCounter = meterRegistry.counter("validation.success");
        this.validationFailureCounter = meterRegistry.counter("validation.failure");
        this.validationTimer = meterRegistry.timer("validation.time");
    }
    
    public Map<String, Object> validateMessage(byte[] message) {
        return validationTimer.record(() -> {
            Map<String, Object> result = new HashMap<>();
            
            try {
                if (message.length < 5) {
                    throw new IllegalArgumentException("Message too short");
                }
                
                // Extract schema ID from message (Confluent wire format)
                ByteBuffer buffer = ByteBuffer.wrap(message);
                byte magic = buffer.get();
                
                if (magic != MAGIC_BYTE) {
                    throw new IllegalArgumentException("Invalid magic byte");
                }
                
                int schemaId = buffer.getInt();
                
                // Get compiled schema
                Schema schema = schemaClient.getCompiledAvroSchema((long) schemaId);
                
                // Validate payload
                byte[] payload = new byte[message.length - 5];
                buffer.get(payload);
                
                GenericDatumReader<Object> reader = new GenericDatumReader<>(schema);
                Object decoded = reader.read(null, DecoderFactory.get().binaryDecoder(
                    new ByteArrayInputStream(payload), null));
                
                result.put("valid", true);
                result.put("schemaId", schemaId);
                result.put("decoded", decoded.toString());
                
                validationSuccessCounter.increment();
                
            } catch (Exception e) {
                log.warn("Validation failed: {}", e.getMessage());
                result.put("valid", false);
                result.put("error", e.getMessage());
                validationFailureCounter.increment();
            }
            
            return result;
        });
    }
    
    public Map<String, Object> validateWithExplicitSchema(byte[] payload, Long schemaId) {
        return validationTimer.record(() -> {
            Map<String, Object> result = new HashMap<>();
            
            try {
                Schema schema = schemaClient.getCompiledAvroSchema(schemaId);
                
                GenericDatumReader<Object> reader = new GenericDatumReader<>(schema);
                Object decoded = reader.read(null, DecoderFactory.get().binaryDecoder(
                    new ByteArrayInputStream(payload), null));
                
                result.put("valid", true);
                result.put("schemaId", schemaId);
                result.put("decoded", decoded.toString());
                
                validationSuccessCounter.increment();
                
            } catch (Exception e) {
                log.warn("Validation failed: {}", e.getMessage());
                result.put("valid", false);
                result.put("error", e.getMessage());
                validationFailureCounter.increment();
            }
            
            return result;
        });
    }
}
