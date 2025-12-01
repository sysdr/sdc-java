package com.example.logprocessor.coordinator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StorageCoordinatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(StorageCoordinatorApplication.class, args);
    }
}
