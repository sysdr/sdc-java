package com.example.logprocessor.coordinator;

import com.example.logprocessor.common.DistributionMetrics;
import com.example.logprocessor.common.NodeInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/coordinator")
@RequiredArgsConstructor
public class CoordinatorController {
    
    private final ConsistentHashRing hashRing;
    private final RingMembershipService membershipService;
    private final DistributionMetricsService metricsService;
    
    @GetMapping("/nodes")
    public Set<String> getNodes() {
        return hashRing.getPhysicalNodes();
    }
    
    @GetMapping("/nodes/info")
    public List<NodeInfo> getNodeInfo() {
        return membershipService.getNodeInfoList();
    }
    
    @GetMapping("/metrics/distribution")
    public DistributionMetrics getDistributionMetrics() {
        return metricsService.calculateDistribution();
    }
    
    @GetMapping("/node/{key}")
    public String getNodeForKey(@PathVariable String key) {
        return hashRing.getNode(key);
    }
    
    @GetMapping("/ring/stats")
    public RingStats getRingStats() {
        return new RingStats(
            hashRing.getPhysicalNodes().size(),
            hashRing.getVirtualNodeCount(),
            150
        );
    }
    
    record RingStats(int physicalNodes, int virtualNodes, int virtualNodesPerNode) {}
}
