package com.example.logprocessor.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class LogConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogConsumerApplication.class, args);
    }
}
