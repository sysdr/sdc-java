package com.example.logprocessor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "log_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String messageId;
    
    @Column(nullable = false)
    private String level;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false)
    private String source;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private Instant processedAt;
    
    @Column(nullable = false)
    private Long processorEpoch;
    
    @Column(nullable = false)
    private String processorInstanceId;
}
