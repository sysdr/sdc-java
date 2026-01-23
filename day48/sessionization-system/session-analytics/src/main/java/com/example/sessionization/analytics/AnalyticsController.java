package com.example.sessionization.analytics;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final SessionRepository repository;

    public AnalyticsController(SessionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/sessions/user/{userId}")
    public List<SessionEntity> getUserSessions(@PathVariable String userId) {
        return repository.findByUserIdOrderByStartTimeDesc(userId);
    }

    @GetMapping("/sessions/converted")
    public List<SessionEntity> getConvertedSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return repository.findByHasConversion(true, PageRequest.of(page, size)).getContent();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(@RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", repository.findRecentSessions(since).size());
        stats.put("averageDuration", repository.getAverageDuration(since));
        stats.put("averageEventCount", repository.getAverageEventCount(since));
        stats.put("conversionCount", repository.getConversionCount(since));
        
        long total = repository.findRecentSessions(since).size();
        long converted = repository.getConversionCount(since);
        stats.put("conversionRate", total > 0 ? (double) converted / total : 0.0);
        
        return stats;
    }

    @GetMapping("/sessions/recent")
    public List<SessionEntity> getRecentSessions(@RequestParam(defaultValue = "1") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return repository.findRecentSessions(since);
    }
}
