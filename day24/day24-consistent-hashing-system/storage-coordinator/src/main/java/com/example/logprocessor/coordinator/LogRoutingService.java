package com.example.logprocessor.coordinator;

import com.example.logprocessor.common.LogEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Routes logs from Kafka to storage nodes based on consistent hashing.
 * Uses circuit breakers for fault tolerance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogRoutingService {
    
    private final ConsistentHashRing hashRing;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    
    @KafkaListener(topics = "distributed-logs", groupId = "storage-coordinator")
    public void routeLog(LogEvent event) {
        try {
            // Get routing key (use source IP for locality)
            String routingKey = event.getSourceIp();
            
            // Get target node from hash ring
            String targetNode = hashRing.getNode(routingKey);
            
            if (targetNode == null) {
                log.warn("No nodes available in ring, dropping log {}", event.getId());
                meterRegistry.counter("logs.dropped.no_nodes").increment();
                return;
            }
            
            // Set routing metadata
            event.setAssignedNode(targetNode);
            
            // Route to storage node with circuit breaker
            routeToNode(targetNode, event);
            
            // Metrics
            Counter.builder("logs.routed")
                .tag("target_node", targetNode)
                .register(meterRegistry)
                .increment();
            
        } catch (Exception e) {
            log.error("Failed to route log {}", event.getId(), e);
            meterRegistry.counter("logs.routing.failed").increment();
        }
    }
    
    @CircuitBreaker(name = "storageNode", fallbackMethod = "routeToFallback")
    private void routeToNode(String nodeId, LogEvent event) {
        String url = String.format("http://%s:8082/api/storage/store", nodeId);
        restTemplate.postForObject(url, event, Void.class);
        log.debug("Routed log {} to node {}", event.getId(), nodeId);
    }
    
    private void routeToFallback(String nodeId, LogEvent event, Exception e) {
        log.warn("Circuit breaker open for node {}, trying next node", nodeId);
        
        // Get next node in ring for replication
        List<String> replicas = hashRing.getNodesForReplication(event.getSourceIp(), 2);
        if (replicas.size() > 1) {
            String fallbackNode = replicas.get(1);
            try {
                routeToNode(fallbackNode, event);
                log.info("Successfully routed to fallback node {}", fallbackNode);
            } catch (Exception ex) {
                log.error("Fallback routing also failed", ex);
                meterRegistry.counter("logs.routing.fallback_failed").increment();
            }
        }
    }
}
