package com.example.storagenode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A value with its version vector for conflict detection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionedValue {
    private String value;
    private VersionVector version;
    private String nodeId;
    private Instant timestamp;
}
