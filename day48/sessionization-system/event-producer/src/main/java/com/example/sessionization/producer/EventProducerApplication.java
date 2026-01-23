package com.example.sessionization.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventProducerApplication.class, args);
    }
}
