package com.example.coordinator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "quorum")
@Data
public class QuorumProperties {
    private List<String> replicas = new ArrayList<>();
    private Timeout timeout = new Timeout();
    
    @Data
    public static class Timeout {
        private long ms = 100;
    }
}

