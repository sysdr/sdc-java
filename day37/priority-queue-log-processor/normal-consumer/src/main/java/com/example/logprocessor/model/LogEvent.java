package com.example.logprocessor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "normal_logs", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_priority", columnList = "priority")
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
    
    @Enumerated(EnumType.STRING)
    private PriorityLevel priority;
}
