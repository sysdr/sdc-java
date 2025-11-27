package com.example.enrichment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafkaStreams
@EnableScheduling
public class EnrichmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnrichmentServiceApplication.class, args);
    }
}
