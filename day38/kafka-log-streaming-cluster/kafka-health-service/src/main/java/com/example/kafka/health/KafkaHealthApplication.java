package com.example.kafka.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KafkaHealthApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaHealthApplication.class, args);
    }
}
