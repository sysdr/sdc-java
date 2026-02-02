package com.example.logproducer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "log-events";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter messagesProduced;
    private final Counter messagesFailed;
    private final Timer sendTimer;
    
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.messagesProduced = Counter.builder("kafka.messages.produced")
                .description("Total messages produced to Kafka")
                .register(meterRegistry);
        this.messagesFailed = Counter.builder("kafka.messages.failed")
                .description("Failed message sends")
                .register(meterRegistry);
        this.sendTimer = Timer.builder("kafka.send.duration")
                .description("Time taken to send messages")
                .register(meterRegistry);
    }
    
    public CompletableFuture<SendResult<String, String>> sendLogEvent(LogEvent logEvent) {
        return sendTimer.record(() -> {
            try {
                String json = objectMapper.writeValueAsString(logEvent);
                
                CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(TOPIC, logEvent.getId(), json);
                
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        messagesProduced.increment();
                        logger.debug("Sent log event: {} to partition {}", 
                            logEvent.getId(), result.getRecordMetadata().partition());
                    } else {
                        messagesFailed.increment();
                        logger.error("Failed to send log event: {}", logEvent.getId(), ex);
                    }
                });
                
                return future;
            } catch (JsonProcessingException e) {
                messagesFailed.increment();
                throw new RuntimeException("Failed to serialize log event", e);
            }
        });
    }
}
