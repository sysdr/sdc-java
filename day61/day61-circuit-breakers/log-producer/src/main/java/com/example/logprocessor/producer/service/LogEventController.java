package com.example.logprocessor.producer.service;

import com.example.logprocessor.producer.model.LogEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal endpoint — not exposed publicly.
 * Only api-gateway calls this endpoint (via service-to-service HTTP).
 */
@RestController
@RequestMapping("/api/logs/internal")
public class LogEventController {

    private final KafkaProducerService kafkaProducerService;

    public LogEventController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/produce")
    public ResponseEntity<Map<String, String>> produce(@RequestBody LogEvent event) {
        kafkaProducerService.produce(event);
        return ResponseEntity.ok(Map.of(
                "status", "queued",
                "eventId", event.eventId()
        ));
    }

    /**
     * Trigger dead-letter buffer drain manually.
     * In production this would be called by a reconciliation job.
     */
    @PostMapping("/dlq/drain")
    public ResponseEntity<Map<String, Object>> drainDLQ() {
        var drained = kafkaProducerService.drainDeadLetterBuffer();
        return ResponseEntity.ok(Map.of(
                "drained", drained.size(),
                "status", "drained"
        ));
    }
}
