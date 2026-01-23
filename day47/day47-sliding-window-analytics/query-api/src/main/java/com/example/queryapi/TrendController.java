package com.example.queryapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TrendController {
    
    private final QueryService queryService;
    
    public TrendController(QueryService queryService) {
        this.queryService = queryService;
    }
    
    @GetMapping("/trends/{serviceId}")
    public ResponseEntity<TrendResponse> getTrends(@PathVariable("serviceId") String serviceId) {
        TrendResponse response = queryService.getTrends(serviceId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
