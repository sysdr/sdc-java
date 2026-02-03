package com.example.logprocessor.model;

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
public class ConsumerState {
    private String instanceId;
    private boolean isLeader;
    private Long epoch;
    private Instant lastHeartbeat;
    private Map<Integer, Long> partitionOffsets;
    private long totalMessagesProcessed;
    private Instant stateTimestamp;
}
