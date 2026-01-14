package com.example.stateproducer.controller;

import com.example.stateproducer.model.EntityState;
import com.example.stateproducer.service.StateProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/state")
@RequiredArgsConstructor
public class StateController {

    private final StateProducerService stateProducerService;

    @PostMapping("/update")
    public CompletableFuture<ResponseEntity<Map<String, String>>> updateState(
            @RequestBody EntityState state) {
        log.info("Received state update request: {}", state.getEntityId());
        
        return stateProducerService.updateEntityState(state)
            .thenApply(result -> ResponseEntity.ok(Map.of(
                "status", "accepted",
                "entityId", state.getEntityId(),
                "partition", String.valueOf(result.getRecordMetadata().partition()),
                "offset", String.valueOf(result.getRecordMetadata().offset())
            )))
            .exceptionally(ex -> {
                log.error("State update failed", ex);
                return ResponseEntity.internalServerError()
                    .body(Map.of("status", "failed", "error", ex.getMessage()));
            });
    }

    @DeleteMapping("/{entityType}/{entityId}")
    public CompletableFuture<ResponseEntity<Map<String, String>>> deleteState(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        log.info("Received state deletion request: {} {}", entityType, entityId);
        
        return stateProducerService.deleteEntityState(entityId, entityType)
            .thenApply(result -> ResponseEntity.ok(Map.of(
                "status", "deleted",
                "entityId", entityId,
                "entityType", entityType
            )))
            .exceptionally(ex -> {
                log.error("State deletion failed", ex);
                return ResponseEntity.internalServerError()
                    .body(Map.of("status", "failed", "error", ex.getMessage()));
            });
    }

    @PostMapping("/update/sync")
    public ResponseEntity<Map<String, String>> updateStateSync(
            @RequestBody EntityState state) {
        log.info("Received synchronous state update request: {}", state.getEntityId());
        
        try {
            stateProducerService.updateEntityStateSync(state, 5);
            return ResponseEntity.ok(Map.of(
                "status", "confirmed",
                "entityId", state.getEntityId()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }
}
