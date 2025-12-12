package com.example.storagenode;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Versioned log entry with vector clock for conflict detection
 */
@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_key", columnList = "entryKey")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entryKey;

    @Column(nullable = false, length = 10000)
    private String value;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String nodeId;

    // Store version vector as JSON
    @Column(nullable = false, columnDefinition = "TEXT")
    private String versionVectorJson;

    @Transient
    private VersionVector versionVector;

    public LogEntry(String entryKey, String value, String nodeId, VersionVector versionVector) {
        this.entryKey = entryKey;
        this.value = value;
        this.nodeId = nodeId;
        this.timestamp = Instant.now();
        this.versionVector = versionVector;
    }
}
