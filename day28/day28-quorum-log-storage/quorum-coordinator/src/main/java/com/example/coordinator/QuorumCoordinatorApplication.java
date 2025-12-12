package com.example.coordinator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(QuorumProperties.class)
public class QuorumCoordinatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuorumCoordinatorApplication.class, args);
    }
}
