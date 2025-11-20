package com.example.validationgateway.controller;

import com.example.validationgateway.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/validate")
public class ValidationController {
    
    private final ValidationService validationService;
    
    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> validateMessage(@RequestBody byte[] message) {
        return ResponseEntity.ok(validationService.validateMessage(message));
    }
    
    @PostMapping("/schema/{schemaId}")
    public ResponseEntity<Map<String, Object>> validateWithSchema(
            @PathVariable Long schemaId,
            @RequestBody byte[] payload) {
        return ResponseEntity.ok(validationService.validateWithExplicitSchema(payload, schemaId));
    }
}
