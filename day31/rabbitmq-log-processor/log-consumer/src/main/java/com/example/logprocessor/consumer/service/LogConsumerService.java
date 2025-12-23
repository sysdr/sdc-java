package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEvent;
import com.example.logprocessor.consumer.model.ProcessedLog;
import com.example.logprocessor.consumer.repository.ProcessedLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LogConsumerService {

    private final ProcessedLogRepository repository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Counter messagesProcessed;
    private final Counter messagesFailed;
    private final Timer processingTimer;

    public LogConsumerService(
            ProcessedLogRepository repository,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagesProcessed = Counter.builder("rabbitmq.messages.processed")
            .description("Total messages processed successfully")
            .register(meterRegistry);
        this.messagesFailed = Counter.builder("rabbitmq.messages.failed")
            .description("Total messages failed to process")
            .register(meterRegistry);
        this.processingTimer = Timer.builder("rabbitmq.processing.duration")
            .description("Message processing duration")
            .register(meterRegistry);
    }

    @RabbitListener(queues = "logs-critical", ackMode = "MANUAL")
    @Transactional
    public void processCriticalLogs(
            LogEvent logEvent,
            com.rabbitmq.client.Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        
        processLogWithAck(logEvent, channel, tag, "CRITICAL");
    }

    @RabbitListener(queues = "logs-processing", ackMode = "MANUAL")
    @Transactional
    public void processGeneralLogs(
            LogEvent logEvent,
            com.rabbitmq.client.Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        
        processLogWithAck(logEvent, channel, tag, "GENERAL");
    }

    @RabbitListener(queues = "logs-monitoring", ackMode = "MANUAL")
    @Transactional
    public void processMonitoringLogs(
            LogEvent logEvent,
            com.rabbitmq.client.Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        
        processLogWithAck(logEvent, channel, tag, "MONITORING");
    }

    private void processLogWithAck(
            LogEvent logEvent,
            com.rabbitmq.client.Channel channel,
            long tag,
            String queueType) throws Exception {
        
        Timer.Sample sample = Timer.start();
        
        try {
            // Idempotency check
            if (repository.existsById(logEvent.getId())) {
                log.debug("Duplicate message detected: {}", logEvent.getId());
                channel.basicAck(tag, false);
                return;
            }

            // Process and persist log
            ProcessedLog processedLog = ProcessedLog.builder()
                .id(logEvent.getId())
                .severity(logEvent.getSeverity())
                .category(logEvent.getCategory())
                .message(logEvent.getMessage())
                .source(logEvent.getSource())
                .timestamp(logEvent.getTimestamp())
                .processedAt(Instant.now())
                .metadataJson(serializeMetadata(logEvent.getMetadata()))
                .build();

            repository.save(processedLog);

            // Cache recent log for API queries
            cacheLog(logEvent);

            // Acknowledge message
            channel.basicAck(tag, false);
            messagesProcessed.increment();
            
            log.debug("Processed {} log: {}", queueType, logEvent.getId());
            
        } catch (Exception e) {
            messagesFailed.increment();
            log.error("Failed to process log {}", logEvent.getId(), e);
            // Negative acknowledge with requeue
            channel.basicNack(tag, false, true);
        } finally {
            sample.stop(processingTimer);
        }
    }

    private void cacheLog(LogEvent logEvent) {
        try {
            String key = "log:" + logEvent.getId();
            String value = objectMapper.writeValueAsString(logEvent);
            redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache log {}", logEvent.getId(), e);
        }
    }

    private String serializeMetadata(Object metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
