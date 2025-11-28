package com.example.logproducer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String source;
    private String message;
    private LogLevel level;
    private LocalDateTime timestamp;
    private String traceId;
    
    // Partition routing metadata
    private String partitionKey;  // For Kafka topic partitioning
    private Integer sourceHash;   // For database partitioning
}
