package com.example.storage.controller;

import com.example.storage.model.LogEntry;
import com.example.storage.model.ReplicationRequest;
import com.example.storage.model.ReplicationResponse;
import com.example.storage.repository.RocksDBRepository;
import com.example.storage.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/storage")
public class StorageController {
    
    private final RocksDBRepository repository;
    private final ReplicationService replicationService;
    
    public StorageController(RocksDBRepository repository, 
                            ReplicationService replicationService) {
        this.repository = repository;
        this.replicationService = replicationService;
    }
    
    @PostMapping("/write")
    public ResponseEntity<?> writeEntry(@RequestBody LogEntry entry,
                                       @RequestParam(value = "followers", required = false) List<String> followers,
                                       @RequestParam(value = "generationId", defaultValue = "0") int generationId) {
        try {
            entry.setTimestamp(Instant.now());
            entry.setVersion(System.currentTimeMillis());
            
            // If this is a leader write with followers, replicate
            if (followers != null && !followers.isEmpty()) {
                boolean quorumAchieved = replicationService.replicateToFollowers(
                    entry, followers, generationId
                );
                
                if (!quorumAchieved) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Failed to achieve write quorum");
                }
            } else {
                // Direct write (follower or single-node)
                repository.put(entry.getKey(), entry);
            }
            
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            log.error("Failed to write entry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Write failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/read/{key}")
    public ResponseEntity<?> readEntry(@PathVariable("key") String key) {
        try {
            return repository.get(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to read entry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Read failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/replicate")
    public ResponseEntity<ReplicationResponse> handleReplication(
            @RequestBody ReplicationRequest request) {
        try {
            ReplicationResponse response = replicationService.handleReplication(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to handle replication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ReplicationResponse.builder()
                    .requestId(request.getRequestId())
                    .success(false)
                    .build());
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("HEALTHY");
    }
}
