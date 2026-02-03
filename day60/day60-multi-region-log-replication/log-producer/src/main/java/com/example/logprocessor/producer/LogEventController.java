package com.example.logprocessor.producer;

import com.example.logprocessor.producer.model.LogEvent;
import com.example.logprocessor.producer.model.ProduceRequest;
import com.example.logprocessor.producer.model.ProduceResponse;
import com.example.logprocessor.producer.service.KafkaProducerService;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for log ingestion.
 *
 * POST /logs          – produce a single event
 * POST /logs/batch    – produce a batch (up to 500 events)
 *
 * The controller is intentionally thin: validation, event construction,
 * and Kafka publishing are delegated. This keeps the HTTP layer testable
 * without a running Kafka broker.
 */
@RestController
@RequestMapping("/logs")
public class LogEventController {

    private static final Logger log = LoggerFactory.getLogger(LogEventController.class);
    private static final int MAX_BATCH_SIZE = 500;

    private final KafkaProducerService producerService;
    private final String region;

    public LogEventController(
            KafkaProducerService producerService,
            @Value("${app.region}") String region
    ) {
        this.producerService = producerService;
        this.region = region;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ProduceResponse> produceEvent(@Valid @RequestBody ProduceRequest request) {
        LogEvent event = LogEvent.create(region, request.serviceName(), request.level(), request.message(), request.correlationId());

        producerService.publish(event);

        return ResponseEntity.accepted().body(new ProduceResponse(
                event.eventId(),
                region,
                "log-events-" + region,
                true,
                "Event accepted for publishing"
        ));
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ProduceResponse> produceBatch(@RequestBody java.util.List<ProduceRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().body(new ProduceResponse(null, region, null, false, "Batch is empty"));
        }
        if (requests.size() > MAX_BATCH_SIZE) {
            return ResponseEntity.badRequest().body(new ProduceResponse(null, region, null, false, "Batch exceeds max size of " + MAX_BATCH_SIZE));
        }

        int published = 0;
        for (ProduceRequest req : requests) {
            LogEvent event = LogEvent.create(region, req.serviceName(), req.level(), req.message(), req.correlationId());
            producerService.publish(event);
            published++;
        }

        log.info("Batch of {} events accepted in region {}", published, region);
        return ResponseEntity.accepted().body(new ProduceResponse(
                null,
                region,
                "log-events-" + region,
                true,
                "Batch of " + published + " events accepted"
        ));
    }

    /** Health-probe endpoint used by the API Gateway for region routing decisions. */
    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, String>> health() {
        return ResponseEntity.ok(java.util.Map.of("status", "UP", "region", region));
    }
}
