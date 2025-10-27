package com.example.logprocessor.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableCaching
public class LogProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogProducerApplication.class, args);
    }
}
