package com.example.coordinator.controller;

import com.example.coordinator.model.LogEntry;
import com.example.coordinator.model.LogQuery;
import com.example.coordinator.service.QueryCoordinatorService;
import com.example.coordinator.service.PartitionMetadataCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
public class QueryController {
    
    private final QueryCoordinatorService coordinatorService;
    private final PartitionMetadataCache metadataCache;
    
    @PostMapping(value = "/logs", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<LogEntry> queryLogs(@RequestBody LogQuery query) {
        log.info("Received query: {}", query);
        return coordinatorService.executeQuery(query);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQueryStats() {
        return ResponseEntity.ok(Map.of(
            "totalPartitions", metadataCache.getPartitionCount(),
            "status", "healthy"
        ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
