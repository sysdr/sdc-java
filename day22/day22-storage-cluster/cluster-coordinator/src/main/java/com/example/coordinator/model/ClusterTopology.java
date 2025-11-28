package com.example.coordinator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterTopology {
    private int generationId;
    private String leaderId;
    private List<NodeMetadata> nodes;
    private Instant lastUpdate;
}
