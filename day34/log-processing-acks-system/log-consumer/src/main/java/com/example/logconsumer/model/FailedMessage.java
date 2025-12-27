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
@Table(name = "failed_messages")
public class FailedMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String messageId;
    private String originalTopic;
    
    @Column(columnDefinition = "TEXT")
    private String messageContent;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private String exceptionType;
    private int retryCount;
    private Instant failedAt;
}
