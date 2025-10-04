package com.example.logserver.service;

import com.example.logserver.model.LogEntry;
import com.example.logserver.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for querying logs and monitoring server health.
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogQueryController {

    private final LogRepository logRepository;
    private final LogBufferService bufferService;

    @GetMapping("/search")
    public ResponseEntity<List<LogEntry>> searchLogs(
            @RequestParam("level") String level,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        List<LogEntry> logs = logRepository.findByLevelAndTimestampBetween(
            level, start, end
        );
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<LogEntry>> getLogsBySource(@PathVariable String source) {
        List<LogEntry> logs = logRepository.findTop100BySourceOrderByTimestampDesc(source);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        List<Object[]> counts = logRepository.countByLevelBetween(start, end);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("buffer_size", bufferService.getBufferSize());
        stats.put("dropped_count", bufferService.getDroppedCount());
        
        Map<String, Long> levelCounts = new HashMap<>();
        for (Object[] row : counts) {
            levelCounts.put((String) row[0], (Long) row[1]);
        }
        stats.put("level_counts", levelCounts);
        
        return ResponseEntity.ok(stats);
    }
}
