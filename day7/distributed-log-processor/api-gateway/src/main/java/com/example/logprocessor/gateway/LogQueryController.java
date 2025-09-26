package com.example.logprocessor.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*")
public class LogQueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogQueryController.class);
    
    private final LogQueryService logQueryService;
    private final MeterRegistry meterRegistry;
    private final Counter queryCounter;
    private final Timer queryTimer;
    
    public LogQueryController(LogQueryService logQueryService, MeterRegistry meterRegistry) {
        this.logQueryService = logQueryService;
        this.meterRegistry = meterRegistry;
        this.queryCounter = Counter.builder("log_queries_total")
                .description("Total number of log queries")
                .register(meterRegistry);
        this.queryTimer = Timer.builder("log_query_duration")
                .description("Time spent executing log queries")
                .register(meterRegistry);
    }
    
    @GetMapping("/logs")
    @CircuitBreaker(name = "log-query", fallbackMethod = "fallbackQueryLogs")
    public ResponseEntity<Page<LogEventEntity>> queryLogs(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int size
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            queryCounter.increment();
            logger.info("Querying logs: startTime={}, endTime={}, level={}, source={}, page={}, size={}", 
                       startTime, endTime, level, source, page, size);
            
            if (startTime == null) startTime = LocalDateTime.now().minusDays(1);
            if (endTime == null) endTime = LocalDateTime.now();
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<LogEventEntity> logs = logQueryService.queryLogs(startTime, endTime, level, source, pageable);
            
            return ResponseEntity.ok(logs);
        } finally {
            sample.stop(Timer.builder("log_query_duration").register(meterRegistry));
        }
    }
    
    @GetMapping("/search")
    @CircuitBreaker(name = "log-query", fallbackMethod = "fallbackSearchLogs")
    public ResponseEntity<Page<LogEventEntity>> searchLogs(
            @RequestParam String query,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int size
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            queryCounter.increment();
            logger.info("Searching logs: query={}, startTime={}, endTime={}, page={}, size={}", 
                       query, startTime, endTime, page, size);
            
            if (startTime == null) startTime = LocalDateTime.now().minusDays(1);
            if (endTime == null) endTime = LocalDateTime.now();
            
            Pageable pageable = PageRequest.of(page, size);
            Page<LogEventEntity> logs = logQueryService.searchLogs(query, startTime, endTime, pageable);
            
            return ResponseEntity.ok(logs);
        } finally {
            sample.stop(Timer.builder("log_query_duration").register(meterRegistry));
        }
    }
    
    @GetMapping("/stats")
    @Cacheable(value = "logStats", key = "#hoursBack")
    @CircuitBreaker(name = "log-query", fallbackMethod = "fallbackGetStats")
    public ResponseEntity<Map<String, Object>> getLogStats(
            @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hoursBack
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            queryCounter.increment();
            logger.info("Getting log statistics for last {} hours", hoursBack);
            
            LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
            Map<String, Object> stats = logQueryService.getLogStats(since);
            
            return ResponseEntity.ok(stats);
        } finally {
            sample.stop(Timer.builder("log_query_duration").register(meterRegistry));
        }
    }
    
    @GetMapping("/logs/{id}")
    @CircuitBreaker(name = "log-query", fallbackMethod = "fallbackGetLogById")
    public ResponseEntity<LogEventEntity> getLogById(@PathVariable String id) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            queryCounter.increment();
            logger.info("Getting log by ID: {}", id);
            
            return logQueryService.getLogById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } finally {
            sample.stop(Timer.builder("log_query_duration").register(meterRegistry));
        }
    }
    
    // Circuit breaker fallback methods
    public ResponseEntity<Page<LogEventEntity>> fallbackQueryLogs(
            LocalDateTime startTime, LocalDateTime endTime, String level, String source, 
            int page, int size, Exception ex
    ) {
        logger.warn("Circuit breaker activated for log query: {}", ex.getMessage());
        return ResponseEntity.status(503)
                .header("X-Fallback", "Query service temporarily unavailable")
                .body(Page.empty());
    }
    
    public ResponseEntity<Page<LogEventEntity>> fallbackSearchLogs(
            String query, LocalDateTime startTime, LocalDateTime endTime, 
            int page, int size, Exception ex
    ) {
        logger.warn("Circuit breaker activated for log search: {}", ex.getMessage());
        return ResponseEntity.status(503)
                .header("X-Fallback", "Search service temporarily unavailable")
                .body(Page.empty());
    }
    
    public ResponseEntity<Map<String, Object>> fallbackGetStats(int hoursBack, Exception ex) {
        logger.warn("Circuit breaker activated for log stats: {}", ex.getMessage());
        return ResponseEntity.status(503)
                .header("X-Fallback", "Statistics service temporarily unavailable")
                .body(Map.of("error", "Service temporarily unavailable"));
    }
    
    public ResponseEntity<LogEventEntity> fallbackGetLogById(String id, Exception ex) {
        logger.warn("Circuit breaker activated for log retrieval: {}", ex.getMessage());
        return ResponseEntity.status(503)
                .header("X-Fallback", "Log retrieval service temporarily unavailable")
                .build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "api-gateway",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
