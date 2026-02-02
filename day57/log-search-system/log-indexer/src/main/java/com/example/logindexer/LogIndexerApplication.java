package com.example.logindexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogIndexerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogIndexerApplication.class, args);
    }
}
