package com.example.logprocessor.gateway.controller;

import com.example.logprocessor.gateway.model.LogEvent;
import com.example.logprocessor.gateway.service.LogQueryService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/query")
public class LogQueryController {

    private static final Logger logger = LoggerFactory.getLogger(LogQueryController.class);

    private final LogQueryService logQueryService;
    private final Counter queriesCounter;
    private final Timer queryTimer;

    @Autowired
    public LogQueryController(LogQueryService logQueryService, MeterRegistry meterRegistry) {
        this.logQueryService = logQueryService;
        this.queriesCounter = Counter.builder("log_queries_total")
                .description("Total number of log queries executed")
                .register(meterRegistry);
        this.queryTimer = Timer.builder("log_query_duration")
                .description("Time taken to execute log queries")
                .register(meterRegistry);
    }

    @GetMapping("/logs")
    @CircuitBreaker(name = "log-query", fallbackMethod = "fallbackLogQuery")
    public ResponseEntity<Page<LogEvent>> queryLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Timer.Sample sample = Timer.start();
        try {
            queriesCounter.increment();
            
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<LogEvent> result = logQueryService.queryLogs(level, source, keyword, startTime, endTime, pageable);
            
            logger.info("Query executed successfully: level={}, source={}, keyword={}, results={}", 
                       level, source, keyword, result.getTotalElements());
            
            return ResponseEntity.ok(result);
            
        } finally {
            sample.stop(queryTimer);
        }
    }

    @GetMapping("/logs/{traceId}")
    @CircuitBreaker(name = "log-query", fallbackMethod = "fallbackSingleLogQuery")
    public ResponseEntity<LogEvent> getLogByTraceId(@PathVariable String traceId) {
        Timer.Sample sample = Timer.start();
        try {
            queriesCounter.increment();
            
            LogEvent logEvent = logQueryService.getLogByTraceId(traceId);
            
            if (logEvent != null) {
                logger.debug("Found log event for trace ID: {}", traceId);
                return ResponseEntity.ok(logEvent);
            } else {
                logger.debug("No log event found for trace ID: {}", traceId);
                return ResponseEntity.notFound().build();
            }
            
        } finally {
            sample.stop(queryTimer);
        }
    }

    @GetMapping("/logs/stats")
    @CircuitBreaker(name = "log-query", fallbackMethod = "fallbackLogStats")
    public ResponseEntity<Map<String, Object>> getLogStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        Timer.Sample sample = Timer.start();
        try {
            queriesCounter.increment();
            
            Map<String, Object> stats = logQueryService.getLogStatistics(since);
            
            logger.info("Log statistics generated for period since: {}", since);
            
            return ResponseEntity.ok(stats);
            
        } finally {
            sample.stop(queryTimer);
        }
    }

    @GetMapping("/logs/search")
    @CircuitBreaker(name = "log-query", fallbackMethod = "fallbackLogSearch")
    public ResponseEntity<List<LogEvent>> searchLogs(
            @RequestParam String query,
            @RequestParam(defaultValue = "100") int limit) {
        
        Timer.Sample sample = Timer.start();
        try {
            queriesCounter.increment();
            
            List<LogEvent> results = logQueryService.searchLogs(query, limit);
            
            logger.info("Search executed successfully: query={}, results={}", query, results.size());
            
            return ResponseEntity.ok(results);
            
        } finally {
            sample.stop(queryTimer);
        }
    }

    // Circuit breaker fallback methods
    public ResponseEntity<Page<LogEvent>> fallbackLogQuery(String level, String source, String keyword, 
                                                         LocalDateTime startTime, LocalDateTime endTime, 
                                                         int page, int size, String sortBy, String sortDir, 
                                                         Exception ex) {
        logger.warn("Circuit breaker activated for log query. Fallback triggered.", ex);
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<LogEvent> fallbackSingleLogQuery(String traceId, Exception ex) {
        logger.warn("Circuit breaker activated for single log query. Fallback triggered.", ex);
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<Map<String, Object>> fallbackLogStats(LocalDateTime since, Exception ex) {
        logger.warn("Circuit breaker activated for log stats. Fallback triggered.", ex);
        return ResponseEntity.status(503).build();
    }

    public ResponseEntity<List<LogEvent>> fallbackLogSearch(String query, int limit, Exception ex) {
        logger.warn("Circuit breaker activated for log search. Fallback triggered.", ex);
        return ResponseEntity.status(503).build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("API Gateway Service is healthy");
    }
}
