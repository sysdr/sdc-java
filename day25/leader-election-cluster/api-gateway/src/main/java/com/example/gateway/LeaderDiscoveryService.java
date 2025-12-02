package com.example.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@Slf4j
public class LeaderDiscoveryService {
    
    @Value("${storage.nodes}")
    private List<String> storageNodes;
    
    private volatile String currentLeader;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Boolean> nodeHealth = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        discoverLeader();
    }
    
    @Scheduled(fixedRate = 5000)
    public String discoverLeader() {
        for (String node : storageNodes) {
            try {
                String url = String.format("%s/write/leader", node);
                LeaderInfo response = restTemplate.getForObject(url, LeaderInfo.class);
                
                if (response != null && response.isLeader()) {
                    if (!node.equals(currentLeader)) {
                        log.info("Discovered new leader: {}", node);
                        currentLeader = node;
                    }
                    nodeHealth.put(node, true);
                    return currentLeader;
                } else if (response != null && response.leaderId() != null) {
                    currentLeader = response.leaderId();
                    return currentLeader;
                }
                
                nodeHealth.put(node, true);
            } catch (Exception e) {
                log.warn("Failed to contact node {}: {}", node, e.getMessage());
                nodeHealth.put(node, false);
            }
        }
        
        return currentLeader;
    }
    
    public void updateLeader(String leaderId) {
        log.info("Updating leader to: {}", leaderId);
        this.currentLeader = leaderId;
    }
    
    public List<String> getClusterNodes() {
        return storageNodes;
    }
    
    public Map<String, Boolean> getNodeHealth() {
        return Map.copyOf(nodeHealth);
    }
}

record LeaderInfo(boolean isLeader, String leaderId, long term) {}
