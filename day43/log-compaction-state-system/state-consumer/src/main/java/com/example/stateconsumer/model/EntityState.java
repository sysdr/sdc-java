package com.example.stateconsumer.model;

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
@Table(name = "entity_states",
       indexes = {
           @Index(name = "idx_entity_type", columnList = "entityType"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_timestamp", columnList = "timestamp")
       })
public class EntityState {
    
    @Id
    private String entityId;
    
    @Column(nullable = false)
    private String entityType;
    
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String attributes;  // JSON string
    
    @Column(nullable = false)
    private Instant timestamp;
    
    private Long version;
    
    @Column(nullable = false)
    private Instant updatedAt;
}
