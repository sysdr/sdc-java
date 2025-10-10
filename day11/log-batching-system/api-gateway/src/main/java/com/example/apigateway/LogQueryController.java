package com.example.apigateway;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class LogQueryController {
    
    private final LogRepository logRepository;
    
    @GetMapping("/recent")
    @Cacheable("recent-logs")
    public List<LogEntry> getRecentLogs() {
        return logRepository.findRecent();
    }
    
    @GetMapping("/service/{service}")
    @Cacheable(value = "service-logs", key = "#service")
    public List<LogEntry> getLogsByService(@PathVariable String service) {
        return logRepository.findByServiceOrderByTimestampDesc(service);
    }
    
    @GetMapping("/level/{level}")
    @Cacheable(value = "level-logs", key = "#level")
    public List<LogEntry> getLogsByLevel(@PathVariable String level) {
        return logRepository.findByLevelOrderByTimestampDesc(level);
    }
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
