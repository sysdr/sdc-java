package com.example.logindexing.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogGeneratorScheduler {

    private final LogProducerService producerService;

    public LogGeneratorScheduler(LogProducerService producerService) {
        this.producerService = producerService;
    }

    @Scheduled(fixedRate = 100) // Generate 10 logs per second
    public void generateLogs() {
        LogEvent logEvent = producerService.generateRandomLog();
        producerService.produceLog(logEvent);
    }
}
