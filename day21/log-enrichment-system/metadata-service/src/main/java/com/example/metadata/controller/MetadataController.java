package com.example.metadata.controller;

import com.example.metadata.model.HostMetadataDTO;
import com.example.metadata.model.ServiceMetadataDTO;
import com.example.metadata.service.MetadataManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {
    
    private final MetadataManagementService metadataService;
    
    @GetMapping("/hosts")
    public ResponseEntity<List<HostMetadataDTO>> getAllHosts() {
        return ResponseEntity.ok(metadataService.getAllHosts());
    }
    
    @PostMapping("/hosts")
    public ResponseEntity<HostMetadataDTO> createHost(@RequestBody HostMetadataDTO dto) {
        return ResponseEntity.ok(metadataService.createHost(dto));
    }
    
    @GetMapping("/services")
    public ResponseEntity<List<ServiceMetadataDTO>> getAllServices() {
        return ResponseEntity.ok(metadataService.getAllServices());
    }
    
    @PostMapping("/services")
    public ResponseEntity<ServiceMetadataDTO> createService(@RequestBody ServiceMetadataDTO dto) {
        return ResponseEntity.ok(metadataService.createService(dto));
    }
}
