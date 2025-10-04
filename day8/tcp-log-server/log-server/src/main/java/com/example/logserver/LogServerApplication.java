package com.example.logserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for TCP Log Server.
 * 
 * This application provides:
 * - TCP server on port 9090 for receiving log streams
 * - REST API on port 8080 for health checks and queries
 * - Metrics endpoint for Prometheus scraping
 * - PostgreSQL persistence with batched writes
 */
@SpringBootApplication
@EnableScheduling
public class LogServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogServerApplication.class, args);
    }
}
