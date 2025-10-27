package com.example.logprocessor.producer;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/logs")
public class LogEventController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogEventController.class);
    
    private final SchemaValidationService validationService;
    private final KafkaProducerService producerService;
    
    public LogEventController(SchemaValidationService validationService,
                             KafkaProducerService producerService) {
        this.validationService = validationService;
        this.producerService = producerService;
    }
    
    /**
     * Ingest single log event with validation
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingestLog(@Valid @RequestBody LogEvent event) {
        
        // Perform schema validation
        SchemaValidationService.ValidationResult validationResult = 
            validationService.validate(event);
        
        Map<String, Object> response = new HashMap<>();
        
        if (validationResult.isValid()) {
            // Send to Kafka
            producerService.sendLog(event);
            
            response.put("status", "accepted");
            response.put("message", "Log event accepted for processing");
            logger.info("Log event accepted: {}", event);
            
            return ResponseEntity.accepted().body(response);
            
        } else {
            // Send to DLQ
            producerService.sendToDLQ(event, validationResult.getErrorMessage());
            
            response.put("status", "rejected");
            response.put("message", "Log event failed schema validation");
            response.put("error", validationResult.getErrorMessage());
            logger.warn("Log event rejected: {}", validationResult.getErrorMessage());
            
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
    }
    
    /**
     * Batch ingest with validation
     */
    @PostMapping("/ingest/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(@Valid @RequestBody List<LogEvent> events) {
        
        int accepted = 0;
        int rejected = 0;
        
        for (LogEvent event : events) {
            SchemaValidationService.ValidationResult result = validationService.validate(event);
            
            if (result.isValid()) {
                producerService.sendLog(event);
                accepted++;
            } else {
                producerService.sendToDLQ(event, result.getErrorMessage());
                rejected++;
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", events.size());
        response.put("accepted", accepted);
        response.put("rejected", rejected);
        
        logger.info("Batch processing complete: {} accepted, {} rejected", accepted, rejected);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "log-producer");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}
