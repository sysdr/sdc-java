package com.example.logindexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class LogEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(LogEventConsumer.class);
    
    private final BulkIndexerService bulkIndexer;
    private final ObjectMapper objectMapper;
    private final Counter messagesConsumed;
    private final Counter consumerErrors;
    
    public LogEventConsumer(BulkIndexerService bulkIndexer,
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.bulkIndexer = bulkIndexer;
        this.objectMapper = objectMapper;
        this.messagesConsumed = Counter.builder("kafka.messages.consumed")
                .description("Total messages consumed from Kafka")
                .register(meterRegistry);
        this.consumerErrors = Counter.builder("kafka.consumer.errors")
                .description("Consumer processing errors")
                .register(meterRegistry);
    }
    
    @KafkaListener(topics = "log-events", groupId = "log-indexer-group")
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            bulkIndexer.addToBuffer(event);
            messagesConsumed.increment();
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
            logger.debug("Consumed log event: {}", event.getId());
        } catch (Exception e) {
            consumerErrors.increment();
            logger.error("Failed to process message", e);
        }
    }
}
