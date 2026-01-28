package com.example.indexing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IndexingApplication {
    public static void main(String[] args) {
        SpringApplication.run(IndexingApplication.class, args);
    }
}
