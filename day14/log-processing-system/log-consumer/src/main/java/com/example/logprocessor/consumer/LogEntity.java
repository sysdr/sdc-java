package com.example.logprocessor.consumer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "log_events", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_source", columnList = "source")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntity {
    @Id
    private String id;
    
    private String level;
    
    @Column(length = 2000)
    private String message;
    
    private String source;
    
    private Instant timestamp;
    
    private String traceId;
    
    @Column(name = "processed_at")
    private Instant processedAt;
}
