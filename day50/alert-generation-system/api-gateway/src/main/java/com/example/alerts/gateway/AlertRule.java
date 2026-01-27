package com.example.alerts.gateway;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String name;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private RuleType type;
    
    private String condition;
    private Integer threshold;
    private Integer windowMinutes;
    
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum RuleType {
        ERROR_THRESHOLD,
        LATENCY_THRESHOLD,
        ERROR_RATE,
        CUSTOM
    }
    
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}
