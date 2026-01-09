package com.example.logprocessor.consumer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_logs", indexes = {
    @Index(name = "idx_event_id", columnList = "eventId", unique = true),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_partition", columnList = "partition"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String eventId;
    
    private String source;
    private String level;
    
    @Column(length = 2000)
    private String message;
    
    private String application;
    private String hostname;
    private Instant timestamp;
    private String traceId;
    private Integer partition;
    
    @Column(nullable = false)
    private Instant processedAt = Instant.now();
}
