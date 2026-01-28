package com.example.coordinator.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
@Builder
public class QueryResult {
    private List<Map<String, Object>> rows;
    private long totalRows;
    private long executionTimeMs;
    private String nodeId;
    private boolean fromCache;
}
