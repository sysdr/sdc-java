package com.example.logprocessor.producer.controller;

import com.example.logprocessor.producer.model.LogEventRequest;
import com.example.logprocessor.producer.service.AvroProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/logs")
public class LogEventController {

    private static final Logger logger = LoggerFactory.getLogger(LogEventController.class);

    private final AvroProducerService producerService;

    public LogEventController(AvroProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createLogEvent(
            @RequestBody LogEventRequest request) {
        
        try {
            var result = producerService.produceLogEvent(request)
                .get(5, TimeUnit.SECONDS);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("partition", result.getRecordMetadata().partition());
            response.put("offset", result.getRecordMetadata().offset());
            response.put("schemaVersion", request.schemaVersion());
            
            return ResponseEntity.ok(response);
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Failed to produce log event: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> createBatchLogEvents(
            @RequestBody java.util.List<LogEventRequest> requests) {
        
        int successCount = 0;
        int failCount = 0;
        
        for (LogEventRequest request : requests) {
            try {
                producerService.produceLogEvent(request)
                    .get(5, TimeUnit.SECONDS);
                successCount++;
            } catch (Exception e) {
                failCount++;
                logger.error("Batch item failed: {}", e.getMessage());
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", requests.size());
        response.put("success", successCount);
        response.put("failed", failCount);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/schemas")
    public ResponseEntity<Map<String, Object>> getSchemaInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("v1", producerService.getSchemaV1().toString(true));
        info.put("v2", producerService.getSchemaV2().toString(true));
        return ResponseEntity.ok(info);
    }
}
