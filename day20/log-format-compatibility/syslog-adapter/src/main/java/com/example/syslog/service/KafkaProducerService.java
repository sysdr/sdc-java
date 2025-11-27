package com.example.syslog.service;

import com.example.syslog.model.SyslogMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "raw-syslog-logs";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter messagesProduced;
    private final Counter producerErrors;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.messagesProduced = Counter.builder("syslog.messages.produced")
                .description("Total syslog messages produced to Kafka")
                .register(meterRegistry);
        this.producerErrors = Counter.builder("syslog.producer.errors")
                .description("Syslog producer errors")
                .register(meterRegistry);
    }

    public void sendSyslogMessage(SyslogMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(TOPIC, message.getHostname(), json)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            messagesProduced.increment();
                            logger.debug("Sent syslog message from {}", message.getHostname());
                        } else {
                            producerErrors.increment();
                            logger.error("Failed to send syslog message", ex);
                        }
                    });
        } catch (Exception e) {
            producerErrors.increment();
            logger.error("Error serializing syslog message", e);
        }
    }
}
