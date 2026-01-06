package com.example.kafka.loadtest;

import com.example.kafka.models.LogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LogProducerService {
    private static final Logger logger = LoggerFactory.getLogger(LogProducerService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer sendTimer;
    private final AtomicLong totalSent = new AtomicLong(0);

    private static final String[] SERVICES = {
        "api-gateway", "user-service", "order-service", "payment-service",
        "inventory-service", "notification-service", "analytics-service"
    };
    
    private static final String[] SEVERITIES = {"INFO", "WARN", "ERROR", "DEBUG"};
    private static final String[] MESSAGES = {
        "Request processed successfully",
        "Database connection timeout",
        "Cache hit for user data",
        "Failed to send notification",
        "Order placed successfully",
        "Payment processing initiated",
        "Inventory updated"
    };

    public LogProducerService(KafkaTemplate<String, String> kafkaTemplate, 
                             ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.successCounter = meterRegistry.counter("kafka.producer.success");
        this.errorCounter = meterRegistry.counter("kafka.producer.errors");
        this.sendTimer = meterRegistry.timer("kafka.producer.send.time");
    }

    public void sendLogBatch(int count, String topic) {
        logger.info("Sending {} log events to topic: {}", count, topic);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            try {
                LogEvent event = generateLogEvent();
                sendLogEvent(event, topic);
            } catch (Exception e) {
                logger.error("Error sending log event", e);
                errorCounter.increment();
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        double throughput = (count * 1000.0) / duration;
        
        logger.info("Sent {} events in {}ms ({} events/sec)", count, duration, 
                   String.format("%.2f", throughput));
    }

    private LogEvent generateLogEvent() {
        String serviceName = SERVICES[random.nextInt(SERVICES.length)];
        String severity = SEVERITIES[random.nextInt(SEVERITIES.length)];
        String message = MESSAGES[random.nextInt(MESSAGES.length)];
        
        LogEvent event = new LogEvent(serviceName, severity, message);
        event.setEventId(UUID.randomUUID().toString());
        event.setEnvironment("production");
        
        try {
            event.setHostName(InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            event.setHostName("unknown");
        }
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("requestId", UUID.randomUUID().toString());
        metadata.put("userId", "user-" + random.nextInt(10000));
        event.setMetadata(metadata);
        
        return event;
    }

    private void sendLogEvent(LogEvent event, String topic) throws JsonProcessingException {
        String key = event.getServiceName(); // Partition by service name
        String value = objectMapper.writeValueAsString(event);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        
        Timer.Sample sample = Timer.start();
        
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        
        future.whenComplete((result, ex) -> {
            sample.stop(sendTimer);
            
            if (ex == null) {
                successCounter.increment();
                totalSent.incrementAndGet();
                
                RecordMetadata metadata = result.getRecordMetadata();
                if (totalSent.get() % 1000 == 0) {
                    logger.debug("Sent event to partition {} with offset {}", 
                               metadata.partition(), metadata.offset());
                }
            } else {
                errorCounter.increment();
                logger.error("Failed to send event", ex);
            }
        });
    }

    public long getTotalSent() {
        return totalSent.get();
    }

    public void resetCounters() {
        totalSent.set(0);
    }
}
