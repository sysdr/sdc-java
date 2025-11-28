package com.example.logproducer.service;

import com.example.logproducer.model.LogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private PartitionRouterService partitionRouter;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Value("${kafka.topic.logs}")
    private String logTopic;
    
    private final ObjectMapper objectMapper;
    private final Counter sentCounter;
    private final Counter failedCounter;
    
    public KafkaProducerService(MeterRegistry meterRegistry) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.sentCounter = Counter.builder("logs.produced.total")
                .description("Total logs produced to Kafka")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("logs.produced.failed")
                .description("Failed log production attempts")
                .register(meterRegistry);
    }
    
    /**
     * Send log event to Kafka with partition key routing.
     * Uses partition key to ensure all logs from same source+date go to same partition.
     */
    public CompletableFuture<SendResult<String, String>> sendLog(LogEvent logEvent) {
        try {
            // Enrich event with partition metadata
            String partitionKey = partitionRouter.calculateKafkaPartitionKey(
                logEvent.getSource(), 
                logEvent.getTimestamp()
            );
            logEvent.setPartitionKey(partitionKey);
            logEvent.setSourceHash(partitionRouter.calculateSourceHash(logEvent.getSource()));
            
            String message = objectMapper.writeValueAsString(logEvent);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(logTopic, partitionKey, message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    sentCounter.increment();
                    logger.debug("Sent log to partition: {}, offset: {}", 
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    failedCounter.increment();
                    logger.error("Failed to send log", ex);
                }
            });
            
            return future;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize log event", e);
            failedCounter.increment();
            return CompletableFuture.failedFuture(e);
        }
    }
}
