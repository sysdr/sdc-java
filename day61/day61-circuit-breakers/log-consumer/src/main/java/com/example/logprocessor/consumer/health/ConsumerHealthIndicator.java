package com.example.logprocessor.consumer.health;

import com.example.logprocessor.consumer.service.PostgresWriteService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Exposes PostgreSQL and Redis circuit breaker states plus buffer sizes
 * through the health endpoint.
 */
@Component
public class ConsumerHealthIndicator extends AbstractHealthIndicator {

    private final CircuitBreakerRegistry registry;
    private final PostgresWriteService postgresWriteService;

    public ConsumerHealthIndicator(CircuitBreakerRegistry registry, PostgresWriteService postgresWriteService) {
        this.registry = registry;
        this.postgresWriteService = postgresWriteService;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean anyOpen = false;

        for (CircuitBreaker cb : registry.getAllCircuitBreakers()) {
            builder.withDetail(cb.getName(), Map.of(
                    "state", cb.getState().name(),
                    "failureRate", cb.getMetrics().getFailureRate()
            ));
            if (cb.getState() == CircuitBreaker.State.OPEN) {
                anyOpen = true;
            }
        }

        builder.withDetail("writeBufferSize", postgresWriteService.getWriteBufferSize());

        if (anyOpen) {
            builder.down();
        } else {
            builder.up();
        }
    }
}
