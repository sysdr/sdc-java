package com.example.storagenode;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Version Vector (Vector Clock) for tracking causality and detecting conflicts
 * Each replica maintains a counter for every node that has written to this key
 */
@Data
@NoArgsConstructor
@Embeddable
public class VersionVector {
    private Map<String, Long> vector = new HashMap<>();

    public VersionVector(Map<String, Long> vector) {
        this.vector = new HashMap<>(vector);
    }

    /**
     * Increment the counter for a specific node
     */
    public void increment(String nodeId) {
        vector.put(nodeId, vector.getOrDefault(nodeId, 0L) + 1);
    }

    /**
     * Merge two version vectors (take maximum value for each node)
     */
    public VersionVector merge(VersionVector other) {
        Map<String, Long> merged = new HashMap<>(this.vector);
        other.vector.forEach((node, count) ->
            merged.merge(node, count, Math::max)
        );
        return new VersionVector(merged);
    }

    /**
     * Compare version vectors to determine ordering
     * Returns: 1 if this > other, -1 if this < other, 0 if concurrent
     */
    public int compareTo(VersionVector other) {
        boolean thisGreater = false;
        boolean otherGreater = false;

        // Check all nodes in this vector
        for (Map.Entry<String, Long> entry : vector.entrySet()) {
            String node = entry.getKey();
            long thisCount = entry.getValue();
            long otherCount = other.vector.getOrDefault(node, 0L);

            if (thisCount > otherCount) thisGreater = true;
            if (thisCount < otherCount) otherGreater = true;
        }

        // Check nodes only in other vector
        for (Map.Entry<String, Long> entry : other.vector.entrySet()) {
            if (!vector.containsKey(entry.getKey())) {
                otherGreater = true;
            }
        }

        if (thisGreater && !otherGreater) return 1;  // This dominates
        if (otherGreater && !thisGreater) return -1; // Other dominates
        if (!thisGreater && !otherGreater && vector.equals(other.vector)) return 0; // Equal
        return 0; // Concurrent (conflict)
    }

    /**
     * Check if this vector is concurrent with another (neither dominates)
     */
    public boolean isConcurrentWith(VersionVector other) {
        return compareTo(other) == 0 && !vector.equals(other.vector);
    }

    public VersionVector copy() {
        return new VersionVector(new HashMap<>(vector));
    }
}
