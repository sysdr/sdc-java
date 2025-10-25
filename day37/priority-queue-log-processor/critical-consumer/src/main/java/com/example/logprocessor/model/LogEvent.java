package com.example.logprocessor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "critical_logs", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_service", columnList = "service")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    @Id
    private String id;
    
    @Column(length = 2000)
    private String message;
    
    private String level;
    private String service;
    private Instant timestamp;
    private Instant processedAt;
    private Integer httpStatus;
    private Long latencyMs;
    
    @Column(length = 500)
    private String exception;
    
    @Column(length = 2000)
    private String stackTrace;
    
    @Enumerated(EnumType.STRING)
    private PriorityLevel priority;
    
    private Integer retryCount;
    private Boolean escalated;
}
