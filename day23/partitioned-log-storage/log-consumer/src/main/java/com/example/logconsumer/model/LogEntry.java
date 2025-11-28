package com.example.logconsumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String source;
    
    @Column(nullable = false)
    private Integer sourceHash;
    
    @Column(nullable = false)
    private LocalDateTime logDate;  // Partition key for daily partitions
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogLevel level;
    
    @Column
    private String traceId;
    
    @Column
    private String partitionKey;  // From Kafka
    
    /**
     * Pre-persist hook to calculate partition routing values.
     * Ensures consistency between application logic and database constraints.
     */
    @PrePersist
    public void calculatePartitionKeys() {
        if (this.logDate == null) {
            this.logDate = this.timestamp.toLocalDate().atStartOfDay();
        }
        if (this.sourceHash == null) {
            this.sourceHash = Math.abs(this.source.hashCode() % 256);
        }
    }
}
