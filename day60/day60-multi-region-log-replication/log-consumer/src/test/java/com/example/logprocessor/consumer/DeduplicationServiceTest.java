package com.example.logprocessor.consumer;

import com.example.logprocessor.consumer.service.DeduplicationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeduplicationServiceTest {

    @MockBean
    RedisTemplate<String, String> redisTemplate;

    @Test
    void firstOccurrence_isNotDuplicate() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        DeduplicationService svc = new DeduplicationService(
                redisTemplate, "region-a", 100000, 0.0001, 24, new SimpleMeterRegistry()
        );

        assertThat(svc.isDuplicate("event-uuid-001")).isFalse();
    }

    @Test
    void secondOccurrence_isDuplicate() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // First call: new event
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        DeduplicationService svc = new DeduplicationService(
                redisTemplate, "region-a", 100000, 0.0001, 24, new SimpleMeterRegistry()
        );

        svc.isDuplicate("event-uuid-002"); // registers it
        // Second call: bloom filter will now catch it without hitting Redis
        assertThat(svc.isDuplicate("event-uuid-002")).isTrue();
    }
}
