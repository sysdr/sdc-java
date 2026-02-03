package com.example.logprocessor.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Log Producer microservice.
 * Responsible for ingesting raw log events and publishing them
 * to the region-local Kafka cluster.
 */
@SpringBootApplication
public class LogProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogProducerApplication.class, args);
    }
}
