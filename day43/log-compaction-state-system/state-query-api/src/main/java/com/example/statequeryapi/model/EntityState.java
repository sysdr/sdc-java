package com.example.statequeryapi.model;

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
@Table(name = "entity_states")
public class EntityState {
    
    @Id
    private String entityId;
    private String entityType;
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String attributes;
    
    private Instant timestamp;
    private Long version;
    private Instant updatedAt;
}
