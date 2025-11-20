package com.example.schemaregistry.controller;

import com.example.schemaregistry.dto.SchemaResponse;
import com.example.schemaregistry.service.SchemaRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schemas")
public class SchemaIdController {
    
    private final SchemaRegistryService registryService;
    
    public SchemaIdController(SchemaRegistryService registryService) {
        this.registryService = registryService;
    }
    
    @GetMapping("/ids/{id}")
    public ResponseEntity<SchemaResponse> getSchemaById(@PathVariable Long id) {
        return ResponseEntity.ok(registryService.getSchemaById(id));
    }
}
