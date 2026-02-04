package com.example.logprocessor.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Log Producer — receives events from api-gateway and publishes to Kafka.
 * This is the ONLY service that writes to Kafka in our architecture.
 * Circuit breaker here protects against Kafka broker unavailability.
 */
@SpringBootApplication
public class LogProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogProducerApplication.class, args);
    }
}
