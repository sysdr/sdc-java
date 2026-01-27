package com.example.alerts.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String alertId;
    private String channel;
    
    @Column(length = 2000)
    private String payload;
    
    private String failureReason;
    private Integer retryCount;
    private LocalDateTime failedAt;
    private LocalDateTime nextRetryAt;
    
    @Enumerated(EnumType.STRING)
    private RetryStatus status;
    
    public enum RetryStatus {
        PENDING,
        RETRYING,
        EXHAUSTED,
        SUCCEEDED
    }
}
