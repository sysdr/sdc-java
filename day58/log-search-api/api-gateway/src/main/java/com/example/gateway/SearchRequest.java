package com.example.gateway;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Map;
import java.util.HashMap;

@Data
public class SearchRequest {
    
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Invalid service name")
    private String service;
    
    @Pattern(regexp = "^(DEBUG|INFO|WARN|ERROR|FATAL)$")
    private String level;
    
    private String message;
    
    @Pattern(regexp = "^(15m|1h|6h|24h|7d)$")
    private String timeRange = "1h";
    
    @Min(1)
    @Max(1000)
    private int limit = 100;
    
    private String cursor;
    
    private Map<String, String> filters = new HashMap<>();
    
    /**
     * Estimate query cost for rate limiting
     * Simple queries = 1, wildcards = 5, aggregations = 10
     */
    public int estimateCost() {
        int cost = 1;
        
        if (message != null && message.contains("*")) {
            cost += 5;
        }
        
        if (limit > 500) {
            cost += 2;
        }
        
        return cost;
    }
}
