package com.example.logprocessor.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogEventController {

    private final KafkaProducerService producerService;
    private final Map<String, Integer> sourcePartitionMap = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<Map<String, Object>> createLogEvent(@RequestBody LogEventRequest request) {
        LogEvent event = LogEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .source(request.getSource())
                .level(request.getLevel())
                .message(request.getMessage())
                .application(request.getApplication())
                .hostname(request.getHostname())
                .timestamp(Instant.now())
                .traceId(UUID.randomUUID().toString())
                .build();

        producerService.sendLogEventWithCallback(event, partition -> {
            sourcePartitionMap.put(event.getSource(), partition);
        });

        return ResponseEntity.ok(Map.of(
                "eventId", event.getEventId(),
                "source", event.getSource(),
                "timestamp", event.getTimestamp(),
                "message", "Event queued for processing"
        ));
    }

    @GetMapping("/partition-mapping")
    public ResponseEntity<Map<String, Integer>> getPartitionMapping() {
        return ResponseEntity.ok(sourcePartitionMap);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Producer healthy");
    }
}
