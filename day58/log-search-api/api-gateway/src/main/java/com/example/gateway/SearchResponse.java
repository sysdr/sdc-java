package com.example.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class SearchResponse {
    private List<LogEntry> logs;
    private String nextCursor;
    private long totalHits;
    private long queryTimeMs;
    private boolean truncated;
    
    @Data
    @AllArgsConstructor
    public static class LogEntry {
        private String timestamp;
        private String service;
        private String level;
        private String message;
        private String traceId;
    }
}
