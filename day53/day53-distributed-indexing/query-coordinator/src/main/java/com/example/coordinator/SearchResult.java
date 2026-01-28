package com.example.coordinator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private List<LogEntry> logs;
    private int totalHits;
    private long searchTimeMs;
}
