package com.example.coordinator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuorumReadResult {
    private VersionedValue value;
    private boolean success;
    private List<VersionedValue> conflicts;
}
