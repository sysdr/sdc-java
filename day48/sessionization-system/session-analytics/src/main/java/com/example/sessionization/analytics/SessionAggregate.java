package com.example.sessionization.analytics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionAggregate {
    private String userId;
    private String sessionId;
    private long startTime;
    private long endTime;
    private int eventCount;
    private Map<String, Integer> eventTypeCounts = new HashMap<>();
    private Set<String> pagesVisited = new HashSet<>();
    private boolean hasConversion;
    private String deviceType;
    private String location;
    
    public long getDurationSeconds() {
        return (endTime - startTime) / 1000;
    }
}
