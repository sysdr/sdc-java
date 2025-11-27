package com.example.journald.service;

import com.example.journald.model.JournaldMessage;
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
    private static final String TOPIC = "raw-journald-logs";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter messagesProduced;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.messagesProduced = Counter.builder("journald.messages.produced")
                .description("Total journald messages produced to Kafka")
                .register(meterRegistry);
    }

    public void sendJournaldMessage(JournaldMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(TOPIC, message.getHostname(), json);
            messagesProduced.increment();
            logger.debug("Sent journald message");
        } catch (Exception e) {
            logger.error("Error sending journald message", e);
        }
    }
}
