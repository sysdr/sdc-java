package com.example.logprocessor.gateway.controller;

import com.example.logprocessor.gateway.config.ProducerClient;
import com.example.logprocessor.gateway.model.LogEventPayload;
import com.example.logprocessor.gateway.model.LogEventRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Public-facing endpoint. Accepts log events, validates them,
 * stamps a correlation ID, and delegates to ProducerClient.
 */
@RestController
@RequestMapping("/api/logs")
public class LogIngestController {
    private static final Logger LOG = LoggerFactory.getLogger(LogIngestController.class);

    private final ProducerClient producerClient;
    private final Counter ingestCounter;
    private final Counter errorCounter;

    public LogIngestController(ProducerClient producerClient, MeterRegistry meterRegistry) {
        this.producerClient = producerClient;
        this.ingestCounter = Counter.builder("gateway.log.ingest.total")
                .description("Total log events received at the gateway")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("gateway.log.ingest.errors.total")
                .description("Total log ingest validation or processing errors")
                .register(meterRegistry);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> ingest(@Valid @RequestBody LogEventRequest request) {
        String correlationId = UUID.randomUUID().toString();
        ingestCounter.increment();

        LogEventPayload payload = LogEventPayload.from(request, correlationId);
        LOG.info("[{}] Ingesting log event from source={}, level={}",
                correlationId, payload.source(), payload.level());

        try {
            producerClient.produce(payload);
            return ResponseEntity.ok(Map.of(
                    "status", "accepted",
                    "eventId", payload.eventId(),
                    "correlationId", correlationId
            ));
        } catch (Exception ex) {
            errorCounter.increment();
            LOG.error("[{}] Failed to forward event: {}", correlationId, ex.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to process event",
                    "correlationId", correlationId
            ));
        }
    }

    /**
     * Health probe — used by load balancers and Docker health checks.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "api-gateway"));
    }
}
