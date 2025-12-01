package com.example.logprocessor.coordinator;

import com.example.logprocessor.common.DistributionMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Collects and exposes distribution metrics for monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributionMetricsService {
    
    private final ConsistentHashRing hashRing;
    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    private static final String LOG_COUNT_PREFIX = "ring:logs:";
    
    @Scheduled(fixedRate = 30000)
    public void updateMetrics() {
        try {
            DistributionMetrics metrics = calculateDistribution();
            
            // Expose as Prometheus metrics
            metrics.getLogsPerNode().forEach((nodeId, count) -> {
                Gauge.builder("storage.node.log_count", () -> count)
                    .tag("node", nodeId)
                    .register(meterRegistry);
            });
            
            Gauge.builder("storage.distribution.balance_score", () -> metrics.getBalanceScore())
                .register(meterRegistry);
            
            log.info("Distribution metrics - Balance: {:.2f}%, Total logs: {}", 
                metrics.getBalanceScore(), metrics.getTotalLogs());
            
        } catch (Exception e) {
            log.error("Failed to update distribution metrics", e);
        }
    }
    
    public DistributionMetrics calculateDistribution() {
        Set<String> nodes = hashRing.getPhysicalNodes();
        Map<String, Long> logsPerNode = new HashMap<>();
        long totalLogs = 0;
        
        for (String nodeId : nodes) {
            String key = LOG_COUNT_PREFIX + nodeId;
            String countStr = redisTemplate.opsForValue().get(key);
            long count = countStr != null ? Long.parseLong(countStr) : 0;
            logsPerNode.put(nodeId, count);
            totalLogs += count;
        }
        
        double balance = hashRing.calculateBalance(logsPerNode);
        double balanceScore = Math.max(0, 100 - balance); // Higher is better
        
        String mostLoaded = logsPerNode.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
        
        String leastLoaded = logsPerNode.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
        
        return DistributionMetrics.builder()
            .totalNodes(nodes.size())
            .virtualNodesPerNode(150)
            .totalLogs(totalLogs)
            .logsPerNode(logsPerNode)
            .standardDeviation(balance)
            .balanceScore(balanceScore)
            .mostLoadedNode(mostLoaded)
            .leastLoadedNode(leastLoaded)
            .build();
    }
}
