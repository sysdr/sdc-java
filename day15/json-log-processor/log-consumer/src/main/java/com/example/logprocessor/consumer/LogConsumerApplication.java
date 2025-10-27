package com.example.logprocessor.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
public class LogConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogConsumerApplication.class, args);
    }
}
