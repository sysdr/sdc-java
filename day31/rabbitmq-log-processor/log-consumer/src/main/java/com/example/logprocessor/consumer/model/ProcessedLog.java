package com.example.logprocessor.consumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_logs", indexes = {
    @Index(name = "idx_severity", columnList = "severity"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_category", columnList = "category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedLog {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String severity;
    
    @Column(nullable = false)
    private String category;
    
    @Column(length = 4000)
    private String message;
    
    private String source;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private Instant processedAt;
    
    @Column(length = 2000)
    private String metadataJson;
}
