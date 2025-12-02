package com.example.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ClusterConfig.class)
public class StorageNodeApplication {
    public static void main(String[] args) {
        SpringApplication.run(StorageNodeApplication.class, args);
    }
}
