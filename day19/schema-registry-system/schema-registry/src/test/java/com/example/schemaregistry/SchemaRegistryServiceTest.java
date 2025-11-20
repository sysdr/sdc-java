package com.example.schemaregistry;

import com.example.schemaregistry.dto.RegisterSchemaRequest;
import com.example.schemaregistry.dto.RegisterSchemaResponse;
import com.example.schemaregistry.dto.SchemaResponse;
import com.example.schemaregistry.entity.SchemaType;
import com.example.schemaregistry.service.SchemaRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SchemaRegistryServiceTest {
    
    @Autowired
    private SchemaRegistryService registryService;
    
    private static final String TEST_AVRO_SCHEMA = """
        {
            "type": "record",
            "name": "LogEvent",
            "namespace": "com.example",
            "fields": [
                {"name": "timestamp", "type": "long"},
                {"name": "level", "type": "string"},
                {"name": "message", "type": "string"}
            ]
        }
        """;
    
    @Test
    void shouldRegisterAndRetrieveSchema() {
        // Given
        String subject = "test-logs-value";
        RegisterSchemaRequest request = new RegisterSchemaRequest();
        request.setSchema(TEST_AVRO_SCHEMA);
        request.setSchemaType(SchemaType.AVRO);
        
        // When
        RegisterSchemaResponse registerResponse = registryService.registerSchema(subject, request);
        
        // Then
        assertNotNull(registerResponse.getId());
        
        SchemaResponse schema = registryService.getSchemaById(registerResponse.getId());
        assertEquals(subject, schema.getSubject());
        assertEquals(1, schema.getVersion());
        assertEquals(SchemaType.AVRO, schema.getSchemaType());
    }
    
    @Test
    void shouldReturnExistingSchemaIdForDuplicate() {
        // Given
        String subject = "duplicate-test";
        RegisterSchemaRequest request = new RegisterSchemaRequest();
        request.setSchema(TEST_AVRO_SCHEMA);
        request.setSchemaType(SchemaType.AVRO);
        
        // When
        RegisterSchemaResponse first = registryService.registerSchema(subject, request);
        RegisterSchemaResponse second = registryService.registerSchema(subject, request);
        
        // Then
        assertEquals(first.getId(), second.getId());
    }
}
