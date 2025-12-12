package com.example.coordinator;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Version Vector for tracking causality (duplicate from storage-node for coordinator)
 */
@Data
@NoArgsConstructor
public class VersionVector {
    private Map<String, Long> vector = new HashMap<>();

    public VersionVector(Map<String, Long> vector) {
        this.vector = new HashMap<>(vector);
    }

    public void increment(String nodeId) {
        vector.put(nodeId, vector.getOrDefault(nodeId, 0L) + 1);
    }

    public VersionVector merge(VersionVector other) {
        Map<String, Long> merged = new HashMap<>(this.vector);
        other.vector.forEach((node, count) ->
            merged.merge(node, count, Math::max)
        );
        return new VersionVector(merged);
    }

    public int compareTo(VersionVector other) {
        boolean thisGreater = false;
        boolean otherGreater = false;

        for (Map.Entry<String, Long> entry : vector.entrySet()) {
            String node = entry.getKey();
            long thisCount = entry.getValue();
            long otherCount = other.vector.getOrDefault(node, 0L);

            if (thisCount > otherCount) thisGreater = true;
            if (thisCount < otherCount) otherGreater = true;
        }

        for (Map.Entry<String, Long> entry : other.vector.entrySet()) {
            if (!vector.containsKey(entry.getKey())) {
                otherGreater = true;
            }
        }

        if (thisGreater && !otherGreater) return 1;
        if (otherGreater && !thisGreater) return -1;
        if (!thisGreater && !otherGreater && vector.equals(other.vector)) return 0;
        return 0;
    }

    public boolean isConcurrentWith(VersionVector other) {
        return compareTo(other) == 0 && !vector.equals(other.vector);
    }
}
