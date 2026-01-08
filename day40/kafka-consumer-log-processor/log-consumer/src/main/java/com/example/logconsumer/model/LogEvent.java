package com.example.logconsumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "log_events", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_service", columnList = "service")
})
public class LogEvent {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String level;
    
    @Column(length = 2000)
    private String message;
    
    @Column(nullable = false)
    private String service;
    
    private String host;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    private String traceId;
    private String userId;
    private String environment;
    private String version;
    
    // Enrichment fields
    private String severity;
    private Long processingTime;
    private Instant processedAt;
    
    @Column(nullable = false)
    private Instant createdAt;
}
