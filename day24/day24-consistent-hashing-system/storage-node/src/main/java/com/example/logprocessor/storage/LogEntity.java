package com.example.logprocessor.storage;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "logs", indexes = {
    @Index(name = "idx_source_ip", columnList = "sourceIp"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_level", columnList = "level")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntity {
    
    @Id
    private String id;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private String level;
    private String source;
    private String sourceIp;
    private Instant timestamp;
    private String application;
    private String environment;
    private String assignedNode;
    private Long hashValue;
}
