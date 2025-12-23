package com.example.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Performance Monitor Service
 * Collects and aggregates metrics from distributed log cluster
 */
@SpringBootApplication
@EnableScheduling
public class PerformanceMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PerformanceMonitorApplication.class, args);
    }
}
