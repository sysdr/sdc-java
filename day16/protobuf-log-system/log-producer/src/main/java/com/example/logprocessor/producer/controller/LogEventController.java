package com.example.logprocessor.producer.controller;

import com.example.logprocessor.producer.dto.LogEventDTO;
import com.example.logprocessor.producer.service.KafkaProducerService;
import com.example.logprocessor.proto.LogEvent;
import com.example.logprocessor.proto.LogLevel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@Slf4j
public class LogEventController {
    
    private final KafkaProducerService kafkaProducerService;
    private final Counter jsonMessagesCounter;
    private final Counter protobufMessagesCounter;
    private final Timer jsonProcessingTimer;
    private final Timer protobufProcessingTimer;
    
    public LogEventController(KafkaProducerService kafkaProducerService, 
                             MeterRegistry meterRegistry) {
        this.kafkaProducerService = kafkaProducerService;
        this.jsonMessagesCounter = Counter.builder("log_events_json_total")
            .description("Total JSON log events received")
            .register(meterRegistry);
        this.protobufMessagesCounter = Counter.builder("log_events_protobuf_total")
            .description("Total Protobuf log events received")
            .register(meterRegistry);
        this.jsonProcessingTimer = Timer.builder("log_events_json_processing")
            .description("JSON processing time")
            .register(meterRegistry);
        this.protobufProcessingTimer = Timer.builder("log_events_protobuf_processing")
            .description("Protobuf processing time")
            .register(meterRegistry);
    }
    
    /**
     * JSON endpoint for backward compatibility
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createLogEventJson(
            @Valid @RequestBody LogEventDTO logEventDTO) {
        
        return jsonProcessingTimer.record(() -> {
            try {
                // Convert DTO to Protobuf
                LogEvent logEvent = convertToProtobuf(logEventDTO);
                
                kafkaProducerService.sendLogEvent(logEvent);
                jsonMessagesCounter.increment();
                
                log.debug("JSON log event published: {}", logEventDTO.getEventId());
                
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                        "status", "accepted",
                        "eventId", logEventDTO.getEventId(),
                        "format", "json"
                    ));
            } catch (Exception e) {
                log.error("Failed to process JSON log event", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
            }
        });
    }
    
    /**
     * Binary protobuf endpoint for high-performance clients
     */
    @PostMapping(value = "/binary", 
                 consumes = "application/x-protobuf",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createLogEventProtobuf(
            @RequestBody byte[] protobufData) {
        
        return protobufProcessingTimer.record(() -> {
            try {
                LogEvent logEvent = LogEvent.parseFrom(protobufData);
                
                kafkaProducerService.sendLogEvent(logEvent);
                protobufMessagesCounter.increment();
                
                log.debug("Protobuf log event published: {}", logEvent.getEventId());
                
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                        "status", "accepted",
                        "eventId", logEvent.getEventId(),
                        "format", "protobuf",
                        "size_bytes", String.valueOf(protobufData.length)
                    ));
            } catch (Exception e) {
                log.error("Failed to parse protobuf log event", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid protobuf format: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "json_messages", jsonMessagesCounter.count(),
            "protobuf_messages", protobufMessagesCounter.count()
        ));
    }
    
    private LogEvent convertToProtobuf(LogEventDTO dto) {
        LogEvent.Builder builder = LogEvent.newBuilder()
            .setEventId(dto.getEventId())
            .setTimestamp(dto.getTimestamp())
            .setLevel(parseLogLevel(dto.getLevel()))
            .setMessage(dto.getMessage())
            .setServiceName(dto.getServiceName());
        
        if (dto.getHost() != null) builder.setHost(dto.getHost());
        if (dto.getEnvironment() != null) builder.setEnvironment(dto.getEnvironment());
        if (dto.getTraceId() != null) builder.setTraceId(dto.getTraceId());
        if (dto.getSpanId() != null) builder.setSpanId(dto.getSpanId());
        if (dto.getTags() != null) builder.putAllTags(dto.getTags());
        if (dto.getCustomFields() != null) builder.putAllCustomFields(dto.getCustomFields());
        
        return builder.build();
    }
    
    private LogLevel parseLogLevel(String level) {
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LogLevel.INFO;
        }
    }
}
