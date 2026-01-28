package com.example.coordinator.service;

import com.example.coordinator.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueryCoordinatorService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);
    
    // Simulated executor nodes registry
    private final List<ExecutorNode> executorNodes = Arrays.asList(
        ExecutorNode.builder()
            .nodeId("executor-1")
            .host("query-executor-1")
            .port(8081)
            .healthy(true)
            .partitionId(0)
            .totalLogs(1000000)
            .build(),
        ExecutorNode.builder()
            .nodeId("executor-2")
            .host("query-executor-2")
            .port(8081)
            .healthy(true)
            .partitionId(1)
            .totalLogs(1000000)
            .build(),
        ExecutorNode.builder()
            .nodeId("executor-3")
            .host("query-executor-3")
            .port(8081)
            .healthy(true)
            .partitionId(2)
            .totalLogs(1000000)
            .build()
    );
    
    public List<Map<String, Object>> executeQuery(OptimizedPlan plan) {
        log.info("Executing query across {} nodes", executorNodes.size());
        
        // Check cache first
        if (plan.isUseCache()) {
            Object cached = redisTemplate.opsForValue().get(plan.getCacheKey());
            if (cached != null) {
                log.info("Cache hit for query: {}", plan.getCacheKey());
                return (List<Map<String, Object>>) cached;
            }
        }
        
        // Execute query on all healthy nodes in parallel
        List<CompletableFuture<QueryResult>> futures = executorNodes.stream()
            .filter(ExecutorNode::isHealthy)
            .map(node -> CompletableFuture.supplyAsync(
                () -> executeOnNode(node, plan),
                executorService
            ))
            .collect(Collectors.toList());
        
        // Aggregate results
        List<Map<String, Object>> aggregated = aggregateResults(futures);
        
        // Cache result
        if (plan.isUseCache()) {
            redisTemplate.opsForValue().set(
                plan.getCacheKey(), 
                aggregated,
                5,
                TimeUnit.MINUTES
            );
        }
        
        return aggregated;
    }
    
    @CircuitBreaker(name = "queryExecutor", fallbackMethod = "fallbackQuery")
    private QueryResult executeOnNode(ExecutorNode node, OptimizedPlan plan) {
        log.info("Executing query on node: {}", node.getNodeId());
        
        try {
            String url = String.format("http://%s:%d/api/execute", node.getHost(), node.getPort());
            
            // In production, this would be actual HTTP call
            // For now, simulate query execution
            Thread.sleep(50 + new Random().nextInt(100)); // Simulate latency
            
            List<Map<String, Object>> results = simulateQueryResults(plan);
            
            return QueryResult.builder()
                .rows(results)
                .totalRows(results.size())
                .executionTimeMs(100)
                .nodeId(node.getNodeId())
                .fromCache(false)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to execute query on node: {}", node.getNodeId(), e);
            throw new RuntimeException("Query execution failed", e);
        }
    }
    
    private QueryResult fallbackQuery(ExecutorNode node, OptimizedPlan plan, Exception e) {
        log.warn("Fallback triggered for node: {}", node.getNodeId());
        return QueryResult.builder()
            .rows(Collections.emptyList())
            .totalRows(0)
            .executionTimeMs(0)
            .nodeId(node.getNodeId())
            .fromCache(false)
            .build();
    }
    
    private List<Map<String, Object>> aggregateResults(List<CompletableFuture<QueryResult>> futures) {
        List<Map<String, Object>> allResults = new ArrayList<>();
        
        futures.forEach(future -> {
            try {
                QueryResult result = future.get(5, TimeUnit.SECONDS);
                allResults.addAll(result.getRows());
            } catch (Exception e) {
                log.error("Failed to get result from node", e);
            }
        });
        
        return allResults;
    }
    
    private List<Map<String, Object>> simulateQueryResults(OptimizedPlan plan) {
        List<Map<String, Object>> results = new ArrayList<>();
        Random random = new Random();
        
        int resultCount = Math.min(plan.getEstimatedResultSize() / 3, 100);
        
        for (int i = 0; i < resultCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("timestamp", System.currentTimeMillis() - random.nextInt(86400000));
            row.put("level", random.nextBoolean() ? "ERROR" : "INFO");
            row.put("service", "service-" + random.nextInt(5));
            row.put("message", "Log message " + i);
            results.add(row);
        }
        
        return results;
    }
}
