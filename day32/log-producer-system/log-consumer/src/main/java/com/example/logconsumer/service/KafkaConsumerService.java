package com.example.logconsumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {
    
    @KafkaListener(topics = "logs", groupId = "log-processor", concurrency = "3")
    public void consume(String message) {
        try {
            log.info("Consumed log: {}", message);
            // Process log - save to database, trigger alerts, etc.
        } catch (Exception e) {
            log.error("Failed to process log", e);
        }
    }
}
