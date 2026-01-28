package com.example.executor.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_service", columnList = "service"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false, length = 20)
    private String level;
    
    @Column(nullable = false, length = 100)
    private String service;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "trace_id", length = 64)
    private String traceId;
    
    @Column(name = "partition_id")
    private Integer partitionId;
}
