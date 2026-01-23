package com.example.logproducer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LogGenerator {
    private static final Logger logger = LoggerFactory.getLogger(LogGenerator.class);
    private static final String[] SERVICES = {
        "api-gateway", "auth-service", "user-service", 
        "payment-service", "notification-service"
    };
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter eventCounter;
    private final Random random = new Random();
    
    public LogGenerator(KafkaTemplate<String, String> kafkaTemplate,
                       ObjectMapper objectMapper,
                       MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventCounter = meterRegistry.counter("log.events.produced");
    }
    
    @Scheduled(fixedDelay = 100)  // Generate events every 100ms (10 events/sec per service)
    public void generateLogEvents() {
        for (String service : SERVICES) {
            try {
                LogEvent event = generateEvent(service);
                String json = objectMapper.writeValueAsString(event);
                
                kafkaTemplate.send("log-events", service, json)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            eventCounter.increment();
                            logger.debug("Sent event for {}: {}", service, json);
                        } else {
                            logger.error("Failed to send event", ex);
                        }
                    });
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event", e);
            }
        }
    }
    
    private LogEvent generateEvent(String serviceId) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        
        // Simulate realistic patterns with occasional spikes
        double baseErrorRate = 0.01;  // 1% baseline
        double errorRate = baseErrorRate;
        if (rand.nextDouble() < 0.05) {  // 5% chance of error spike
            errorRate = rand.nextDouble(0.05, 0.15);
        }
        
        double baseLatency = 50.0;  // 50ms baseline
        double latency = baseLatency + rand.nextGaussian() * 10;
        if (rand.nextDouble() < 0.03) {  // 3% chance of latency spike
            latency = rand.nextDouble(200, 500);
        }
        
        return LogEvent.builder()
            .serviceId(serviceId)
            .timestamp(System.currentTimeMillis())
            .errorRate(Math.max(0, errorRate))
            .latencyMs(Math.max(1, latency))
            .throughput(rand.nextLong(100, 1000))
            .cpuUsage(rand.nextDouble(20, 80))
            .memoryUsage(rand.nextDouble(30, 70))
            .build();
    }
}
