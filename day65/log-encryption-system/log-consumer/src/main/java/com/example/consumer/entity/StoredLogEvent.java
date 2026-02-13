package com.example.consumer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "log_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoredLogEvent {
    
    @Id
    private String eventId;
    
    private String eventType;
    private Instant timestamp;
    private String severity;
    
    @Column(columnDefinition = "jsonb")
    private String publicFields;
    
    @Column(columnDefinition = "jsonb")
    private String encryptedFields;
    
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "created_at")
    private Instant createdAt;
}
