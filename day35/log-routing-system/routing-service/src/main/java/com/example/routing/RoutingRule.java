package com.example.routing;

import lombok.Data;
import java.util.List;
import java.util.regex.Pattern;

@Data
public class RoutingRule {
    private String name;
    private int priority;
    private List<String> severities;
    private List<String> sources;
    private List<String> types;
    private String messagePattern;
    private List<String> destinations;
    
    private transient Pattern compiledPattern;
    
    public boolean matches(LogEvent event) {
        // Check severity
        if (severities != null && !severities.isEmpty()) {
            if (!severities.contains(event.getSeverity())) {
                return false;
            }
        }
        
        // Check source
        if (sources != null && !sources.isEmpty()) {
            if (!sources.contains(event.getSource())) {
                return false;
            }
        }
        
        // Check type
        if (types != null && !types.isEmpty()) {
            if (!types.contains(event.getType())) {
                return false;
            }
        }
        
        // Check message pattern
        if (messagePattern != null && !messagePattern.isEmpty()) {
            if (compiledPattern == null) {
                compiledPattern = Pattern.compile(messagePattern);
            }
            if (!compiledPattern.matcher(event.getMessage()).find()) {
                return false;
            }
        }
        
        return true;
    }
}
