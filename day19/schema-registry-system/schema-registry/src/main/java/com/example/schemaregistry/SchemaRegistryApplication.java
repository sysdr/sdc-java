package com.example.schemaregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class SchemaRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaRegistryApplication.class, args);
    }
}
