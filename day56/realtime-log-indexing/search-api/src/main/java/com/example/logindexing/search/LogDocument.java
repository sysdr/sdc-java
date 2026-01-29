package com.example.logindexing.search;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "log_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogDocument {
    
    @Id
    private String id;
    
    private Instant timestamp;
    private String level;
    private String service;
    
    @Column(length = 2000)
    private String message;
    
    private String userId;
    private String traceId;
    
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
    
    private Instant indexedAt;
    
    @Version
    private Long version;
}
