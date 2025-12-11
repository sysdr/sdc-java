package com.example.partition.controller;

import com.example.partition.entity.LogEntryEntity;
import com.example.partition.service.MetadataService;
import com.example.partition.service.PartitionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/partition")
@RequiredArgsConstructor
public class PartitionController {
    
    private final PartitionQueryService queryService;
    private final MetadataService metadataService;
    
    @PostMapping(value = "/query", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<LogEntryEntity> query(@RequestBody Map<String, Object> queryParams) {
        String logLevel = (String) queryParams.get("logLevel");
        String serviceName = (String) queryParams.get("serviceName");
        Instant startTime = queryParams.get("startTime") != null ? 
            Instant.parse((String) queryParams.get("startTime")) : null;
        Instant endTime = queryParams.get("endTime") != null ?
            Instant.parse((String) queryParams.get("endTime")) : null;
        Integer limit = (Integer) queryParams.get("limit");
        
        return queryService.executeQuery(logLevel, serviceName, startTime, endTime, limit);
    }
    
    @GetMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata() {
        return ResponseEntity.ok(metadataService.getMetadata());
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
