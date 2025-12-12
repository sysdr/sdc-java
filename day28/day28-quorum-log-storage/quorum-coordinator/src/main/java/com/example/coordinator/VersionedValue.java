package com.example.coordinator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionedValue {
    private String value;
    private VersionVector version;
    private String nodeId;
    private Instant timestamp;
}
