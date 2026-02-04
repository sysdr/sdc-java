package com.example.logprocessor.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Log Consumer — reads from Kafka, persists to PostgreSQL, caches in Redis.
 * Two independent circuit breakers: one for PostgreSQL, one for Redis.
 * They are independent because their failure modes are orthogonal.
 */
@SpringBootApplication
public class LogConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogConsumerApplication.class, args);
    }
}
