package com.logprocessor.storage;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_partition_version", columnList = "partition_id,version"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String partitionId;
    
    @Column(nullable = false)
    private Long version;
    
    @Column(nullable = false)
    private Long lamportClock;
    
    @Column(nullable = false, length = 1000)
    private String message;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private String nodeId;
    
    @Column(nullable = false, length = 64)
    private String checksum;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public Long getLamportClock() { return lamportClock; }
    public void setLamportClock(Long lamportClock) { this.lamportClock = lamportClock; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
}
