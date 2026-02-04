package com.example.logprocessor.consumer;

import com.example.logprocessor.consumer.model.LogEventEntity;
import com.example.logprocessor.consumer.repository.LogEventRepository;
import com.example.logprocessor.consumer.service.PostgresWriteService;
import com.example.logprocessor.consumer.service.RedisCacheService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PostgresWriteService — Unit Tests")
class PostgresWriteServiceTest {

    private LogEventRepository repository;
    private RedisCacheService cacheService;
    private PostgresWriteService service;

    @BeforeEach
    void setup() {
        repository = Mockito.mock(LogEventRepository.class);
        cacheService = Mockito.mock(RedisCacheService.class);
        service = new PostgresWriteService(repository, cacheService, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("should persist entity and call cache")
    void persistSuccess() {
        LogEventEntity entity = new LogEventEntity();
        entity.setEventId("evt-1");
        entity.setSource("svc-a");
        entity.setLevel("INFO");
        entity.setMessage("test");
        entity.setTimestamp(Instant.now());

        service.persist(entity);

        verify(repository, times(1)).save(entity);
        verify(cacheService, times(1)).put(entity);
    }

    @Test
    @DisplayName("write buffer should be empty initially")
    void bufferEmpty() {
        assertEquals(0, service.getWriteBufferSize());
    }

    @Test
    @DisplayName("drain should return empty list when buffer is empty")
    void drainEmpty() {
        var drained = service.drainWriteBuffer();
        assertTrue(drained.isEmpty());
    }
}
