package com.example.logindexing.indexer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "log_documents", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_service", columnList = "service")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogDocument {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private String level;
    
    @Column(nullable = false)
    private String service;
    
    @Column(length = 2000)
    private String message;
    
    private String userId;
    private String traceId;
    
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
    
    @Column(nullable = false)
    private Instant indexedAt;
    
    @Version
    private Long version;
}
