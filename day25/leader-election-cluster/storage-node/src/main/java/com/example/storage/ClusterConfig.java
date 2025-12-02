package com.example.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "cluster")
@Data
public class ClusterConfig {
    private String nodes; // Accept as string (comma-separated or YAML list)
    
    public List<String> getNodesList() {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }
        // Handle both comma-separated string and YAML list format
        if (nodes.contains(",")) {
            return Arrays.stream(nodes.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        // If it's a single value, return as list
        return List.of(nodes.trim());
    }
}

