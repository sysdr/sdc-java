package com.example.logconsumer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_service", columnList = "service"),
    @Index(name = "idx_level", columnList = "level")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    @Id
    private String id;
    
    private String level;
    private String service;
    
    @Column(length = 1000)
    private String message;
    
    private String timestamp;
    private String traceId;
}
