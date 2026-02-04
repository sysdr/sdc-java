package com.example.logprocessor.producer.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Custom health indicator that exposes circuit breaker state
 * through the /actuator/health endpoint.
 *
 * This lets Kubernetes liveness probes fail when breakers are open,
 * triggering pod restarts if the situation persists.
 */
@Component
public class KafkaCircuitBreakerHealth extends AbstractHealthIndicator {

    private final CircuitBreakerRegistry registry;

    public KafkaCircuitBreakerHealth(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        registry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.State state = cb.getState();
            builder.withDetail(cb.getName(), Map.of(
                    "state", state.name(),
                    "failureRate", cb.getMetrics().getFailureRate(),
                    "slowCallRate", cb.getMetrics().getSlowCallRate(),
                    "openCircuits", state == CircuitBreaker.State.OPEN ? 1 : 0
            ));

            // If any breaker is OPEN, mark health as DOWN
            if (state == CircuitBreaker.State.OPEN) {
                builder.down();
            }
        });

        // If no breaker is open, we're up
        if (registry.getAllCircuitBreakers().stream()
                .noneMatch(cb -> cb.getState() == CircuitBreaker.State.OPEN)) {
            builder.up();
        }
    }
}
