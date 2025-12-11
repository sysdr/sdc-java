package com.example.partition.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_service_level", columnList = "serviceName, logLevel")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntryEntity {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private String logLevel;
    
    @Column(nullable = false)
    private String serviceName;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private String traceId;
    
    private String partitionId;
}
