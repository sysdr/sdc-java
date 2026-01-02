package com.example.routing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaProducerService {
    
    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final Counter messagesRouted;
    private final Timer routingLatency;
    
    public KafkaProducerService(KafkaTemplate<String, LogEvent> kafkaTemplate, 
                                MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.messagesRouted = Counter.builder("logs.routed")
            .description("Number of logs routed to topics")
            .register(meterRegistry);
        this.routingLatency = Timer.builder("routing.latency")
            .description("Log routing latency")
            .register(meterRegistry);
    }
    
    public void routeLog(LogEvent event, List<String> destinations) {
        Timer.Sample sample = Timer.start();
        
        try {
            // Send to all destinations
            List<CompletableFuture<SendResult<String, LogEvent>>> futures = destinations.stream()
                .map(topic -> kafkaTemplate.send(topic, event.getId(), event))
                .toList();
            
            // Wait for all sends to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    messagesRouted.increment(destinations.size());
                    sample.stop(routingLatency);
                    log.debug("Routed log {} to {} topics", event.getId(), destinations.size());
                })
                .exceptionally(ex -> {
                    log.error("Failed to route log {}", event.getId(), ex);
                    return null;
                });
                
        } catch (Exception e) {
            log.error("Error routing log", e);
            sample.stop(routingLatency);
        }
    }
}
