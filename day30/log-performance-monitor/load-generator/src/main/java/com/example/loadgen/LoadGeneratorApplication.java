package com.example.loadgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Load Generator Service
 * Simulates production traffic patterns for performance testing
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class LoadGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoadGeneratorApplication.class, args);
    }
}
