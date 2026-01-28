package com.example.search;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "log_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDocument {
    
    @Id
    private Long id;
    
    @Column(nullable = false)
    private String level;
    
    @Column(nullable = false)
    private String service;
    
    @Column(length = 2000, nullable = false)
    private String message;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    private String userId;
    private String traceId;
    
    @Column(name = "indexed_at")
    private Instant indexedAt;
}
