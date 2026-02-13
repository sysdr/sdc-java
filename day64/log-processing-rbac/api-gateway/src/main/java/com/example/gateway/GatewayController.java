package com.example.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {
    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final AuditService auditService;
    private final PermissionCache permissionCache;
    private final RestTemplate restTemplate = new RestTemplate();

    public GatewayController(AuditService auditService, PermissionCache permissionCache) {
        this.auditService = auditService;
        this.permissionCache = permissionCache;
    }

    @PostMapping("/logs/query")
    @CircuitBreaker(name = "logQueryService", fallbackMethod = "queryLogsFallback")
    public ResponseEntity<?> queryLogs(@RequestBody LogQueryRequest request, 
                                      HttpServletRequest httpRequest) {
        UserContext user = (UserContext) httpRequest.getAttribute("userContext");

        // Authorization check
        if (!canAccessTeamLogs(user, request.getTeam())) {
            auditService.logAccess(user, "QUERY_LOGS", request.getTeam(), "DENIED", 
                                  request.getQuery(), 0);
            return ResponseEntity.status(403).body(Map.of("error", "Access denied to team: " + request.getTeam()));
        }

        try {
            // Forward to log-query-service
            String url = "http://log-query-service:8083/query";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            Map<String, Object> body = response.getBody();
            int recordCount = body != null ? (int) body.getOrDefault("count", 0) : 0;

            auditService.logAccess(user, "QUERY_LOGS", request.getTeam(), "GRANTED", 
                                  request.getQuery(), recordCount);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Failed to query logs", e);
            auditService.logAccess(user, "QUERY_LOGS", request.getTeam(), "ERROR", 
                                  request.getQuery(), 0);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private boolean canAccessTeamLogs(UserContext user, String team) {
        // ADMIN and SRE can access all teams
        if (user.hasAnyRole("ADMIN", "SRE")) {
            return true;
        }

        // Check if user is in the team
        if (user.isInTeam(team)) {
            return true;
        }

        // Check permission cache
        String resource = "logs-" + team;
        return permissionCache.checkPermission(user.getUserId(), resource);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    // Circuit breaker fallback
    public ResponseEntity<?> queryLogsFallback(LogQueryRequest request, 
                                              HttpServletRequest httpRequest, 
                                              Exception e) {
        log.error("Circuit breaker activated for log query", e);
        return ResponseEntity.status(503).body(
            Map.of("error", "Log query service temporarily unavailable", 
                   "message", "Please try again later")
        );
    }
}
