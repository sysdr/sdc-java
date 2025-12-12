package com.example.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Public API for log operations with configurable consistency
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {
    
    private final GatewayService gatewayService;

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> writeLog(
            @RequestBody LogRequest request,
            @RequestParam(defaultValue = "QUORUM") String consistency) {
        
        log.info("Write log: key={}, consistency={}", request.getKey(), consistency);
        
        return gatewayService.writeLog(request.getKey(), request.getValue(), consistency)
            .map(result -> ResponseEntity.ok(Map.of(
                "success", result.get("success"),
                "message", result.get("success").equals(true) 
                    ? "Log written successfully" 
                    : "Failed to achieve quorum",
                "consistency", consistency,
                "acknowledgedReplicas", result.get("acknowledgedReplicas"),
                "requiredReplicas", result.get("requiredReplicas")
            )));
    }

    @GetMapping("/{key}")
    public Mono<ResponseEntity<Map<String, Object>>> readLog(
            @PathVariable String key,
            @RequestParam(defaultValue = "QUORUM") String consistency) {
        
        log.info("Read log: key={}, consistency={}", key, consistency);
        
        return gatewayService.readLog(key, consistency)
            .map(result -> {
                if (result.get("success").equals(false)) {
                    return ResponseEntity.notFound().build();
                }
                
                return ResponseEntity.ok(Map.of(
                    "key", key,
                    "value", result.get("value"),
                    "consistency", consistency,
                    "conflicts", result.get("conflicts")
                ));
            });
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy"));
    }
}
