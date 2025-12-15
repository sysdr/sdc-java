package com.logprocessor.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    
    @Autowired
    private LogEntryRepository repository;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Value("${node.id}")
    private String nodeId;
    
    private final Counter writeCounter;
    private final Counter readCounter;
    
    public StorageController(MeterRegistry meterRegistry) {
        this.writeCounter = Counter.builder("storage_writes_total")
            .description("Total write operations")
            .register(meterRegistry);
        this.readCounter = Counter.builder("storage_reads_total")
            .description("Total read operations")
            .register(meterRegistry);
    }
    
    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> write(@RequestBody WriteRequest request) {
        try {
            LogEntry entry = new LogEntry();
            entry.setPartitionId(request.getPartitionId());
            entry.setMessage(request.getMessage());
            entry.setTimestamp(Instant.now());
            entry.setNodeId(nodeId);
            
            // Get next version for partition
            Long maxVersion = repository.findMaxVersionByPartitionId(request.getPartitionId())
                .orElse(0L);
            entry.setVersion(maxVersion + 1);
            
            // Get next Lamport clock
            Long maxClock = repository.findMaxLamportClockByNodeId(nodeId).orElse(0L);
            entry.setLamportClock(Math.max(maxClock, request.getLamportClock() != null ? request.getLamportClock() : 0L) + 1);
            
            // Calculate checksum
            String checksum = calculateChecksum(request.getMessage());
            entry.setChecksum(checksum);
            
            LogEntry saved = repository.save(entry);
            writeCounter.increment();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "version", saved.getVersion(),
                "lamportClock", saved.getLamportClock(),
                "nodeId", nodeId,
                "checksum", checksum
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/read/{partitionId}/{version}")
    public ResponseEntity<LogEntry> read(
            @PathVariable String partitionId,
            @PathVariable Long version) {
        readCounter.increment();
        return repository.findByPartitionIdAndVersion(partitionId, version)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/read/{partitionId}/latest")
    public ResponseEntity<LogEntry> readLatest(@PathVariable String partitionId) {
        readCounter.increment();
        List<LogEntry> entries = repository.findByPartitionIdOrderByVersionDesc(partitionId);
        if (entries.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entries.get(0));
    }
    
    @GetMapping("/read/{partitionId}/range")
    public ResponseEntity<List<LogEntry>> readRange(
            @PathVariable String partitionId,
            @RequestParam Long startVersion,
            @RequestParam Long endVersion) {
        readCounter.increment();
        List<LogEntry> entries = repository.findByPartitionIdAndVersionRange(
            partitionId, startVersion, endVersion);
        return ResponseEntity.ok(entries);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "nodeId", nodeId,
            "timestamp", Instant.now().toString()
        ));
    }
    
    private String calculateChecksum(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Checksum calculation failed", e);
        }
    }
}

class WriteRequest {
    private String partitionId;
    private String message;
    private Long lamportClock;
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Long getLamportClock() { return lamportClock; }
    public void setLamportClock(Long lamportClock) { this.lamportClock = lamportClock; }
}
