package com.example.logquery;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
public class LogQueryController {

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(@RequestBody Map<String, Object> request) {
        // Simulate log query results
        String query = (String) request.get("query");
        String team = (String) request.get("team");
        
        List<Map<String, Object>> logs = new ArrayList<>();
        
        // Generate mock log entries
        for (int i = 0; i < 10; i++) {
            Map<String, Object> log = new HashMap<>();
            log.put("timestamp", LocalDateTime.now().minusMinutes(i));
            log.put("level", i % 3 == 0 ? "ERROR" : "INFO");
            log.put("service", team + "-service");
            log.put("message", "Sample log message matching query: " + query);
            log.put("team", team);
            logs.add(log);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("logs", logs);
        response.put("count", logs.size());
        response.put("query", query);
        response.put("team", team);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
