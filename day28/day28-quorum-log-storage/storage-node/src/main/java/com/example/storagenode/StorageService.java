package com.example.storagenode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Storage service implementing versioned key-value storage with vector clocks
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService {
    
    private final LogEntryRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${storage.node.id}")
    private String nodeId;

    /**
     * Write a log entry with version vector
     * Returns the new version vector after write
     */
    @Transactional
    public VersionVector write(String key, String value, VersionVector clientVector) {
        log.info("Writing key={} from node={}", key, nodeId);
        
        // Get existing entries to merge version vectors
        List<LogEntry> existing = repository.findByEntryKey(key);
        
        VersionVector newVector;
        if (existing.isEmpty()) {
            // First write - create new vector
            newVector = clientVector != null ? clientVector.copy() : new VersionVector();
        } else {
            // Merge with existing vectors
            newVector = existing.stream()
                .map(this::deserializeVector)
                .reduce(VersionVector::merge)
                .orElse(new VersionVector());
            
            if (clientVector != null) {
                newVector = newVector.merge(clientVector);
            }
        }
        
        // Increment this node's counter
        newVector.increment(nodeId);
        
        LogEntry entry = new LogEntry(key, value, nodeId, newVector);
        entry.setVersionVectorJson(serializeVector(newVector));
        repository.save(entry);
        
        log.info("Wrote key={} with vector={}", key, newVector.getVector());
        return newVector;
    }

    /**
     * Read all versions of a key
     * Returns list of entries with their version vectors for conflict resolution
     */
    @Transactional(readOnly = true)
    public List<VersionedValue> read(String key) {
        log.info("Reading key={} from node={}", key, nodeId);
        
        List<LogEntry> entries = repository.findByEntryKey(key);
        
        return entries.stream()
            .map(entry -> new VersionedValue(
                entry.getValue(),
                deserializeVector(entry),
                entry.getNodeId(),
                entry.getTimestamp()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get all keys stored on this node
     */
    public List<String> getAllKeys() {
        return repository.findAllKeys();
    }

    /**
     * Health check - returns node status
     */
    public Map<String, Object> getStatus() {
        long entryCount = repository.count();
        return Map.of(
            "nodeId", nodeId,
            "status", "healthy",
            "entryCount", entryCount
        );
    }

    private String serializeVector(VersionVector vector) {
        try {
            return objectMapper.writeValueAsString(vector.getVector());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize version vector", e);
        }
    }

    private VersionVector deserializeVector(LogEntry entry) {
        try {
            Map<String, Long> vectorMap = objectMapper.readValue(
                entry.getVersionVectorJson(),
                new TypeReference<Map<String, Long>>() {}
            );
            return new VersionVector(vectorMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize version vector", e);
        }
    }
}
