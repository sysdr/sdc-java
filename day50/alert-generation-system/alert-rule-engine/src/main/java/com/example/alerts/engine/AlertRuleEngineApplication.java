package com.example.alerts.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class AlertRuleEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertRuleEngineApplication.class, args);
    }
}
