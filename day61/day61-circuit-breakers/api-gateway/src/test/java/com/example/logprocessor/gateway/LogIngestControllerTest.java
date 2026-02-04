package com.example.logprocessor.gateway;

import com.example.logprocessor.gateway.config.ProducerClient;
import com.example.logprocessor.gateway.controller.LogIngestController;
import com.example.logprocessor.gateway.model.LogEventRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LogIngestController — Unit Tests")
class LogIngestControllerTest {

    private ProducerClient producerClient;
    private LogIngestController controller;

    @BeforeEach
    void setup() {
        producerClient = Mockito.mock(ProducerClient.class);
        controller = new LogIngestController(producerClient, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("should return 200 and eventId on successful ingest")
    void successfulIngest() {
        when(producerClient.produce(any())).thenReturn(ResponseEntity.ok().build());

        var request = new LogEventRequest(
                "test-service",
                LogEventRequest.LogLevel.INFO,
                "Application started",
                Instant.now()
        );

        var response = controller.ingest(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().get("eventId"));
        assertNotNull(response.getBody().get("correlationId"));
        verify(producerClient, times(1)).produce(any());
    }

    @Test
    @DisplayName("should return 500 when producer throws")
    void producerFailure() {
        when(producerClient.produce(any())).thenThrow(new RuntimeException("connection refused"));

        var request = new LogEventRequest(
                "test-service",
                LogEventRequest.LogLevel.ERROR,
                "Disk full",
                Instant.now()
        );

        var response = controller.ingest(request);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    @DisplayName("health endpoint returns UP")
    void healthCheck() {
        var response = controller.health();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
    }
}
