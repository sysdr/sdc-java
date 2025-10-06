package com.example.logconsumer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_service", columnList = "service")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String level;
    
    @Column(length = 4096)
    private String message;
    
    @Column(nullable = false)
    private String service;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(name = "trace_id")
    private String traceId;
    
    @Column(name = "created_at")
    private Instant createdAt;
}
