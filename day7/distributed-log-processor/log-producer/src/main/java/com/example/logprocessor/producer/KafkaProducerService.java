package com.example.logprocessor.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;
    private final Counter kafkaSuccessCounter;
    private final Counter kafkaErrorCounter;
    private final Timer kafkaSendTimer;
    private final MeterRegistry meterRegistry;
    
    public KafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${app.kafka.log-events-topic}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.topicName = topicName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        this.kafkaSuccessCounter = Counter.builder("kafka_messages_sent_total")
                .tag("result", "success")
                .description("Total number of successful Kafka messages sent")
                .register(meterRegistry);
        
        this.kafkaErrorCounter = Counter.builder("kafka_messages_sent_total")
                .tag("result", "error")
                .description("Total number of failed Kafka messages sent")
                .register(meterRegistry);
        
        this.kafkaSendTimer = Timer.builder("kafka_send_duration")
                .description("Time spent sending messages to Kafka")
                .register(meterRegistry);
    }
    
    public CompletableFuture<Void> sendLogEvent(LogEvent logEvent) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(logEvent);
            
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    topicName, 
                    logEvent.getSource(), // Use source as partition key for ordering
                    jsonMessage
            );
            
            CompletableFuture<SendResult<String, String>> completableFuture = new CompletableFuture<>();
            
            future.addCallback(
                result -> {
                    sample.stop(kafkaSendTimer);
                    kafkaSuccessCounter.increment();
                    logger.debug("Successfully sent log event {} to partition {}", 
                            logEvent.getId(), result.getRecordMetadata().partition());
                    completableFuture.complete(result);
                },
                throwable -> {
                    sample.stop(kafkaSendTimer);
                    kafkaErrorCounter.increment();
                    logger.error("Failed to send log event {} to Kafka", logEvent.getId(), throwable);
                    completableFuture.completeExceptionally(new RuntimeException("Failed to send message to Kafka", throwable));
                }
            );
            
            return completableFuture.thenApply(result -> null);
            
        } catch (JsonProcessingException e) {
            sample.stop(kafkaSendTimer);
            kafkaErrorCounter.increment();
            logger.error("Failed to serialize log event {}", logEvent.getId(), e);
            return CompletableFuture.failedFuture(new RuntimeException("Failed to serialize log event", e));
        }
    }
}
