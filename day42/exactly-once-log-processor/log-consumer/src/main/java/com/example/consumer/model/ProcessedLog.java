package com.example.consumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_logs", indexes = {
    @Index(name = "idx_event_id", columnList = "eventId", unique = true),
    @Index(name = "idx_processed_at", columnList = "processedAt")
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
    
    private String eventType;
    private String service;
    
    @Column(length = 2000)
    private String message;
    
    private String severity;
    private Instant eventTimestamp;
    private Instant processedAt;
    private String userId;
    private String traceId;
    
    @Column(nullable = false, name = "kafka_partition")
    private Integer partition;
    
    @Column(nullable = false, name = "kafka_offset")
    private Long offset;
}
