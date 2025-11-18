package com.example.logprocessor.common.schema;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SchemaEvolutionService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaEvolutionService.class);

    private final SchemaRegistryClient schemaRegistryClient;

    public SchemaEvolutionService(SchemaRegistryClient schemaRegistryClient) {
        this.schemaRegistryClient = schemaRegistryClient;
    }

    public int registerSchema(String subject, Schema schema) throws IOException, RestClientException {
        AvroSchema avroSchema = new AvroSchema(schema);
        int schemaId = schemaRegistryClient.register(subject, avroSchema);
        logger.info("Registered schema for subject '{}' with ID: {}", subject, schemaId);
        return schemaId;
    }

    public Schema getSchemaById(int schemaId) throws IOException, RestClientException {
        return schemaRegistryClient.getSchemaById(schemaId).rawSchema() instanceof Schema 
            ? (Schema) schemaRegistryClient.getSchemaById(schemaId).rawSchema()
            : null;
    }

    public Schema getLatestSchema(String subject) throws IOException, RestClientException {
        var metadata = schemaRegistryClient.getLatestSchemaMetadata(subject);
        return new Schema.Parser().parse(metadata.getSchema());
    }

    public List<Integer> getAllVersions(String subject) throws IOException, RestClientException {
        return schemaRegistryClient.getAllVersions(subject);
    }

    public boolean testCompatibility(String subject, Schema schema) throws IOException, RestClientException {
        AvroSchema avroSchema = new AvroSchema(schema);
        boolean isCompatible = schemaRegistryClient.testCompatibility(subject, avroSchema);
        
        if (isCompatible) {
            logger.info("Schema is compatible with subject '{}'", subject);
        } else {
            logger.warn("Schema is not compatible with subject '{}'", subject);
        }
        return isCompatible;
    }

    public void setCompatibilityLevel(String subject, String level) throws IOException, RestClientException {
        schemaRegistryClient.updateCompatibility(subject, level);
        logger.info("Set compatibility level for subject '{}' to: {}", subject, level);
    }

    public String getCompatibilityLevel(String subject) throws IOException, RestClientException {
        return schemaRegistryClient.getCompatibility(subject);
    }
}
