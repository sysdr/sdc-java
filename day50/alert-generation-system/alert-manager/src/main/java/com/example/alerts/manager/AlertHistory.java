package com.example.alerts.manager;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String alertId;
    private String ruleId;
    private String ruleName;
    private String service;
    private String severity;
    
    @Column(length = 1000)
    private String message;
    
    private Long count;
    private String fingerprint;
    
    @Enumerated(EnumType.STRING)
    private AlertState state;
    
    private LocalDateTime triggeredAt;
    private LocalDateTime notifiedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    
    private String acknowledgedBy;
    
    public enum AlertState {
        TRIGGERED,
        NOTIFIED,
        ACKNOWLEDGED,
        RESOLVED
    }
}
