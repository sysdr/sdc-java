package com.example.logprocessor.producer;

import com.example.logprocessor.producer.model.LogEvent;
import com.example.logprocessor.producer.service.KafkaProducerService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("KafkaProducerService — Unit Tests")
class KafkaProducerServiceTest {

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, LogEvent> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    private KafkaProducerService service;

    @BeforeEach
    void setup() {
        service = new KafkaProducerService(kafkaTemplate, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("should produce event to Kafka successfully")
    void produceSuccess() {
        SendResult<String, LogEvent> mockResult = Mockito.mock(SendResult.class);
        var mockMeta = Mockito.mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(mockResult.getRecordMetadata()).thenReturn(mockMeta);
        when(mockMeta.partition()).thenReturn(0);
        when(mockMeta.offset()).thenReturn(42L);

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(
                CompletableFuture.completedFuture(mockResult)
        );

        LogEvent event = new LogEvent("evt-1", "svc-a", "INFO", "test", Instant.now(), "corr-1");
        var future = service.produce(event);

        assertNotNull(future);
        verify(kafkaTemplate, times(1)).send("log-events", "svc-a", event);
    }

    @Test
    @DisplayName("dead-letter buffer should be empty initially")
    void dlqBufferEmptyInitially() {
        assertEquals(0, service.getDeadLetterBufferSize());
    }
}
