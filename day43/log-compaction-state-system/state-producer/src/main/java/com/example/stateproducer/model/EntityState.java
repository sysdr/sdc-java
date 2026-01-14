package com.example.stateproducer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityState {
    private String entityId;
    private String entityType;
    private String status;
    private Map<String, Object> attributes;
    private Instant timestamp;
    private Long version;
}
