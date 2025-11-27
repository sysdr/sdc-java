package com.example.gateway.controller;

import com.example.gateway.service.LogQueryService;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class QueryController {
    
    private final LogQueryService queryService;

    public QueryController(LogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "hostname", required = false) String hostname,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        try {
            return queryService.searchLogs(level, source, hostname, limit);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return queryService.getStatistics();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "api-gateway");
    }
}
