package com.example.coordinator;

/**
 * Configurable consistency levels for read and write operations
 */
public enum ConsistencyLevel {
    ONE(1),        // Single replica
    TWO(2),        // Two replicas
    QUORUM(-1),    // Majority (N/2 + 1)
    ALL(-1);       // All replicas

    private final int fixedReplicas;

    ConsistencyLevel(int fixedReplicas) {
        this.fixedReplicas = fixedReplicas;
    }

    /**
     * Calculate required replicas based on total replica count
     */
    public int getRequiredReplicas(int totalReplicas) {
        if (this == QUORUM) {
            return (totalReplicas / 2) + 1;
        } else if (this == ALL) {
            return totalReplicas;
        } else {
            return Math.min(fixedReplicas, totalReplicas);
        }
    }
}
