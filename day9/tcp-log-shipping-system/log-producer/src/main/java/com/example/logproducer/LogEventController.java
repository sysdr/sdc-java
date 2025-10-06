package com.example.logproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogEventController {

    private final TcpLogShipperService tcpLogShipperService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<String> createLogEvent(@RequestBody LogEvent logEvent) {
        try {
            // Enrich log event
            if (logEvent.getId() == null) {
                logEvent.setId(UUID.randomUUID().toString());
            }
            if (logEvent.getTimestamp() == null) {
                logEvent.setTimestamp(Instant.now());
            }
            if (logEvent.getTraceId() == null) {
                logEvent.setTraceId(UUID.randomUUID().toString());
            }

            // Convert to JSON string
            String logJson = objectMapper.writeValueAsString(logEvent);
            
            // Ship to TCP server
            boolean shipped = tcpLogShipperService.shipLog(logJson);
            
            if (shipped) {
                return ResponseEntity.accepted().body("Log queued for shipping");
            } else {
                return ResponseEntity.status(503).body("Buffer full, log dropped");
            }
        } catch (Exception e) {
            log.error("Error processing log event", e);
            return ResponseEntity.internalServerError().body("Error processing log");
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<String> createBatchLogEvents(@RequestBody LogEvent[] events) {
        int accepted = 0;
        int dropped = 0;

        for (LogEvent event : events) {
            try {
                if (event.getId() == null) {
                    event.setId(UUID.randomUUID().toString());
                }
                if (event.getTimestamp() == null) {
                    event.setTimestamp(Instant.now());
                }
                if (event.getTraceId() == null) {
                    event.setTraceId(UUID.randomUUID().toString());
                }

                String logJson = objectMapper.writeValueAsString(event);
                if (tcpLogShipperService.shipLog(logJson)) {
                    accepted++;
                } else {
                    dropped++;
                }
            } catch (Exception e) {
                log.error("Error processing log event in batch", e);
                dropped++;
            }
        }

        return ResponseEntity.ok(String.format("Accepted: %d, Dropped: %d", accepted, dropped));
    }
}
