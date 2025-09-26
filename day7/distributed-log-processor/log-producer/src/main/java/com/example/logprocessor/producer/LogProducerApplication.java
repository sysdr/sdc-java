package com.example.logprocessor.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class LogProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogProducerApplication.class, args);
    }
}
