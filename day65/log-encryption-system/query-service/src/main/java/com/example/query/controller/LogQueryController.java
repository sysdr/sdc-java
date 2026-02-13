package com.example.query.controller;

import com.example.query.model.UserContext;
import com.example.query.service.LogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class LogQueryController {
    
    private final LogQueryService logQueryService;
    
    @GetMapping("/logs")
    public List<Map<String, Object>> queryLogs(
            @RequestParam(required = false) String eventType,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Email") String email) {
        
        UserContext user = new UserContext(userId, role, email);
        return logQueryService.queryLogs(eventType, user);
    }
    
    @GetMapping("/logs/{eventId}")
    public Map<String, Object> getLog(
            @PathVariable String eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Email") String email) {
        
        UserContext user = new UserContext(userId, role, email);
        return logQueryService.getLogById(eventId, user);
    }
}
