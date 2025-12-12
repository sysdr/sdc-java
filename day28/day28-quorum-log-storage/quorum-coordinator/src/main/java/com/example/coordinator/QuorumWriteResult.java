package com.example.coordinator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuorumWriteResult {
    private boolean success;
    private int acknowledgedReplicas;
    private int requiredReplicas;
    private VersionVector version;
}
