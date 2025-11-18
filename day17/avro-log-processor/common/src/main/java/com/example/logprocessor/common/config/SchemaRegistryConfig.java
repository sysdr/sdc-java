package com.example.logprocessor.common.config;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SchemaRegistryConfig {

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${schema.registry.cache.capacity:1000}")
    private int cacheCapacity;

    @Bean
    public SchemaRegistryClient schemaRegistryClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("schema.registry.url", schemaRegistryUrl);
        
        return new CachedSchemaRegistryClient(
            schemaRegistryUrl,
            cacheCapacity,
            configs
        );
    }
}
