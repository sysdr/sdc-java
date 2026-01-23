package com.example.gateway;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/windows")
public class WindowQueryController {
    
    private final WindowQueryService queryService;
    
    public WindowQueryController(WindowQueryService queryService) {
        this.queryService = queryService;
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<WindowResultDTO>> getRecentWindows(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(queryService.getRecentWindows(limit));
    }
    
    @GetMapping("/service/{service}")
    public ResponseEntity<List<WindowResultDTO>> getWindowsByService(
            @PathVariable String service,
            @RequestParam String windowType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(
            queryService.getWindowsByServiceAndTimeRange(service, windowType, from, to)
        );
    }
    
    @GetMapping("/stats")
    public ResponseEntity<WindowStatsDTO> getAggregatedStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(queryService.getAggregatedStats(from, to));
    }
}
