package com.example.logprocessor.producer.service;

import com.example.logprocessor.producer.config.RabbitMQConfig;
import com.example.logprocessor.producer.model.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class RabbitMQProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final Counter messagesPublished;

    public RabbitMQProducerService(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagesPublished = Counter.builder("rabbitmq.messages.published")
            .description("Total messages published to RabbitMQ")
            .register(meterRegistry);
    }

    public void publishLog(LogEvent logEvent) {
        if (logEvent.getId() == null) {
            logEvent.setId(UUID.randomUUID().toString());
        }
        if (logEvent.getTimestamp() == null) {
            logEvent.setTimestamp(Instant.now());
        }

        String routingKey = buildRoutingKey(logEvent);
        
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.TOPIC_EXCHANGE,
                routingKey,
                logEvent,
                message -> {
                    message.getMessageProperties().setCorrelationId(logEvent.getId());
                    message.getMessageProperties().setTimestamp(
                        java.util.Date.from(logEvent.getTimestamp())
                    );
                    return message;
                }
            );
            
            messagesPublished.increment();
            log.debug("Published log event {} with routing key {}", logEvent.getId(), routingKey);
        } catch (Exception e) {
            log.error("Failed to publish log event {}", logEvent.getId(), e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }

    private String buildRoutingKey(LogEvent logEvent) {
        String severity = logEvent.getSeverity() != null ? 
            logEvent.getSeverity().toLowerCase() : "info";
        String category = logEvent.getCategory() != null ? 
            logEvent.getCategory().toLowerCase() : "general";
        
        return String.format("logs.%s.%s", severity, category);
    }
}
