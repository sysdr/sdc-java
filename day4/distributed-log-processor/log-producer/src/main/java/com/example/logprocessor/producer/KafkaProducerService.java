package com.example.logprocessor.producer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${app.kafka.topic.raw-logs}")
    private String rawLogsTopic;
    
    private final Counter logsSentCounter;
    private final Counter logsFailedCounter;
    
    public KafkaProducerService(MeterRegistry meterRegistry) {
        this.logsSentCounter = Counter.builder("logs_sent_total")
            .description("Total number of logs sent to Kafka")
            .register(meterRegistry);
        this.logsFailedCounter = Counter.builder("logs_failed_total")
            .description("Total number of failed log sends")
            .register(meterRegistry);
    }
    
    public void sendRawLog(String logEntry) {
        try {
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(rawLogsTopic, logEntry);
            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    logsSentCounter.increment();
                    logger.debug("Sent log with offset: {}", result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    logsFailedCounter.increment();
                    logger.error("Failed to send log", ex);
                }
            });
        } catch (Exception e) {
            logsFailedCounter.increment();
            logger.error("Error sending log to Kafka", e);
        }
    }
}
