package com.example.gateway;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "window_results")
@Data
public class WindowResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "window_key")
    private String windowKey;
    
    @Column(name = "window_start")
    private Instant windowStart;
    
    @Column(name = "window_end")
    private Instant windowEnd;
    
    @Column(name = "window_type")
    private String windowType;
    
    @Column(name = "event_count")
    private long eventCount;
    
    @Column(name = "error_count")
    private long errorCount;
    
    @Column(name = "warn_count")
    private long warnCount;
    
    @Column(name = "avg_latency_ms")
    private double avgLatencyMs;
    
    @Column(name = "max_latency_ms")
    private int maxLatencyMs;
    
    @Column(name = "min_latency_ms")
    private int minLatencyMs;
    
    @Column(name = "p95_latency_ms")
    private double p95LatencyMs;
    
    @Column(name = "error_rate")
    private double errorRate;
    
    @Column(name = "computed_at")
    private Instant computedAt;
}
