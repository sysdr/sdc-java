package com.systemdesign.logprocessor.consumer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_logs", indexes = {
    @Index(name = "idx_log_id", columnList = "logId"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_application", columnList = "applicationName"),
    @Index(name = "idx_trace_id", columnList = "traceId")
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
    private String logId;

    @Column(nullable = false)
    private String applicationName;

    @Column(nullable = false)
    private String level;

    @Column(length = 4000)
    private String message;

    @Column(nullable = false)
    private Instant timestamp;

    private String host;
    private String service;
    private String traceId;

    @Column(length = 2000)
    private String enrichedData;

    // Kafka metadata
    private Integer partition;
    private Long offset;

    @Column(nullable = false)
    private Instant processedAt;
}
