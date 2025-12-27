package com.example.logconsumer.service;

import com.example.logconsumer.model.FailedMessage;
import com.example.logconsumer.model.LogEvent;
import com.example.logconsumer.repository.FailedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final KafkaTemplate<String, String> dlqKafkaTemplate;
    private final FailedMessageRepository failedMessageRepository;
    private final ObjectMapper objectMapper;
    private final Counter dlqCounter;

    public DeadLetterQueueService(KafkaTemplate<String, String> dlqKafkaTemplate,
                                 FailedMessageRepository failedMessageRepository,
                                 ObjectMapper objectMapper,
                                 MeterRegistry meterRegistry) {
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.failedMessageRepository = failedMessageRepository;
        this.objectMapper = objectMapper;
        this.dlqCounter = meterRegistry.counter("dlq.messages.sent");
    }

    public void sendToDeadLetterQueue(LogEvent event, Exception exception) {
        try {
            String messageJson = objectMapper.writeValueAsString(event);
            
            dlqKafkaTemplate.send("log-events-dlq", event.getId(), messageJson);
            
            FailedMessage failedMessage = FailedMessage.builder()
                .messageId(event.getId())
                .originalTopic("log-events")
                .messageContent(messageJson)
                .errorMessage(exception.getMessage())
                .exceptionType(exception.getClass().getSimpleName())
                .retryCount(5)
                .failedAt(Instant.now())
                .build();
            
            failedMessageRepository.save(failedMessage);
            dlqCounter.increment();
            
            log.info("Message sent to DLQ: id={}", event.getId());
        } catch (Exception e) {
            log.error("Failed to send message to DLQ: {}", event.getId(), e);
        }
    }
}
