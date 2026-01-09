package com.example.logprocessor.consumer;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class GracefulShutdownHandler {

    private final KafkaListenerEndpointRegistry registry;

    @PreDestroy
    public void shutdown() {
        log.info("Initiating graceful shutdown of Kafka consumers");
        
        long startTime = System.currentTimeMillis();
        
        // Stop accepting new messages
        registry.getAllListenerContainers().forEach(container -> {
            log.info("Stopping container: {}", container.getListenerId());
            container.stop();
        });
        
        // Wait for in-flight messages to complete (max 30 seconds)
        boolean allStopped = false;
        try {
            for (int i = 0; i < 30; i++) {
                allStopped = registry.getAllListenerContainers()
                        .stream()
                        .noneMatch(container -> container.isRunning());
                        
                if (allStopped) {
                    break;
                }
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Shutdown interrupted");
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        if (allStopped) {
            log.info("Graceful shutdown completed in {}ms", duration);
        } else {
            log.warn("Force shutdown after {}ms - some messages may be reprocessed", duration);
        }
    }
}
