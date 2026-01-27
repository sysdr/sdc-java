package com.example.alerts.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class LogEventController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @PostMapping("/logs")
    public Map<String, Object> ingestLog(@RequestBody LogEvent logEvent) throws JsonProcessingException {
        logEvent.setTimestamp(String.valueOf(System.currentTimeMillis()));
        if (logEvent.getTraceId() == null) {
            logEvent.setTraceId(UUID.randomUUID().toString());
        }

        String logJson = objectMapper.writeValueAsString(logEvent);
        kafkaTemplate.send("log-events", logEvent.getService(), logJson);

        incrementCounter("logs.ingested", logEvent.getLevel());
        log.debug("Log event ingested: {}", logEvent.getTraceId());

        return Map.of(
            "status", "accepted",
            "traceId", logEvent.getTraceId(),
            "timestamp", logEvent.getTimestamp()
        );
    }

    @PostMapping("/logs/batch")
    public Map<String, Object> ingestBatchLogs(@RequestBody List<LogEvent> logEvents) throws JsonProcessingException {
        int count = 0;
        for (LogEvent logEvent : logEvents) {
            logEvent.setTimestamp(String.valueOf(System.currentTimeMillis()));
            if (logEvent.getTraceId() == null) {
                logEvent.setTraceId(UUID.randomUUID().toString());
            }

            String logJson = objectMapper.writeValueAsString(logEvent);
            kafkaTemplate.send("log-events", logEvent.getService(), logJson);
            count++;
        }

        incrementCounter("logs.batch.ingested", "batch");
        log.info("Batch of {} log events ingested", count);

        return Map.of(
            "status", "accepted",
            "count", count
        );
    }

    private void incrementCounter(String name, String level) {
        Counter.builder(name)
            .tag("level", level)
            .register(meterRegistry)
            .increment();
    }
}
