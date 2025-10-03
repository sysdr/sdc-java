package com.example.logprocessor.gateway;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
public class LogQueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogQueryController.class);
    
    private final LogQueryService logQueryService;

    public LogQueryController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @GetMapping
    @Timed(value = "api_log_query_duration", description = "Time taken for log query API calls")
    public ResponseEntity<Page<LogEntry>> queryLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // Default to last hour if no time range specified
        if (startTime == null) {
            startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        LogQueryRequest request = new LogQueryRequest(startTime, endTime, logLevel, source, keyword, page, size);
        
        logger.debug("Executing log query: {}", request);
        Page<LogEntry> result = logQueryService.queryLogs(request);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/query")
    @Timed(value = "api_log_query_post_duration", description = "Time taken for POST log query API calls")
    public ResponseEntity<Page<LogEntry>> queryLogsPost(@Valid @RequestBody LogQueryRequest request) {
        logger.debug("Executing log query via POST: {}", request);
        Page<LogEntry> result = logQueryService.queryLogs(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    @Timed(value = "api_log_stats_duration", description = "Time taken for log stats API calls")
    public ResponseEntity<Map<String, Object>> getLogStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        // Default to last 24 hours if no time range specified
        if (startTime == null) {
            startTime = Instant.now().minus(24, ChronoUnit.HOURS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        logger.debug("Getting log statistics from {} to {}", startTime, endTime);
        Map<String, Object> stats = logQueryService.getLogStatistics(startTime, endTime);
        
        return ResponseEntity.ok(stats);
    }
}
