package com.example.coordinator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class QueryCoordinatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryCoordinatorApplication.class, args);
    }
}
