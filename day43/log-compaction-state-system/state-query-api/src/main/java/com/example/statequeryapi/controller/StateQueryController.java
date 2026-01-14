package com.example.statequeryapi.controller;

import com.example.statequeryapi.model.EntityState;
import com.example.statequeryapi.service.StateQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class StateQueryController {

    private final StateQueryService queryService;

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<EntityState> getEntityState(@PathVariable String entityId) {
        log.info("Query request for entity: {}", entityId);
        return queryService.getEntityState(entityId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{entityType}")
    public ResponseEntity<List<EntityState>> getStatesByType(
            @PathVariable String entityType) {
        log.info("Query request for entity type: {}", entityType);
        return ResponseEntity.ok(queryService.getStatesByType(entityType));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EntityState>> getStatesByStatus(
            @PathVariable String status) {
        log.info("Query request for status: {}", status);
        return ResponseEntity.ok(queryService.getStatesByStatus(status));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "totalEntities", queryService.getTotalEntityCount()
        ));
    }

    @GetMapping("/stats/type/{entityType}")
    public ResponseEntity<Map<String, Object>> getTypeStats(
            @PathVariable String entityType) {
        return ResponseEntity.ok(Map.of(
            "entityType", entityType,
            "count", queryService.countByType(entityType)
        ));
    }
}
