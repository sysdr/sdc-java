package com.example.logconsumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "log_events")
public class LogEvent {
    @Id
    private String id;
    
    private String level;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private String source;
    private String userId;
    private Instant timestamp;
    private Instant processedAt;
    
    @Transient
    private boolean simulateFailure;
}
