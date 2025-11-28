package com.example.storage.service;

import com.example.storage.model.LogEntry;
import com.example.storage.model.ReplicationRequest;
import com.example.storage.model.ReplicationResponse;
import com.example.storage.repository.RocksDBRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
public class ReplicationService {
    
    private final RocksDBRepository repository;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    private final ExecutorService replicationExecutor;
    
    @Value("${storage.replication.timeout-ms:5000}")
    private long replicationTimeoutMs;
    
    @Value("${storage.replication.factor:3}")
    private int replicationFactor;
    
    @Value("${storage.replication.write-quorum:2}")
    private int writeQuorum;
    
    private final Counter replicationSuccessCounter;
    private final Counter replicationFailureCounter;
    private final Timer replicationLatencyTimer;
    
    public ReplicationService(RocksDBRepository repository, 
                             RestTemplate restTemplate,
                             MeterRegistry meterRegistry) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        this.replicationExecutor = Executors.newFixedThreadPool(10);
        
        this.replicationSuccessCounter = Counter.builder("storage.replication.success")
            .description("Successful replications")
            .register(meterRegistry);
        
        this.replicationFailureCounter = Counter.builder("storage.replication.failure")
            .description("Failed replications")
            .register(meterRegistry);
        
        this.replicationLatencyTimer = Timer.builder("storage.replication.latency")
            .description("Replication latency")
            .register(meterRegistry);
    }
    
    /**
     * Replicate log entry to follower nodes
     * Returns true if write quorum is achieved
     */
    public boolean replicateToFollowers(LogEntry entry, List<String> followerUrls, int generationId) {
        Instant start = Instant.now();
        
        // Store locally first
        repository.put(entry.getKey(), entry);
        int successfulWrites = 1; // Leader counts as one successful write
        
        if (followerUrls.isEmpty()) {
            return successfulWrites >= writeQuorum;
        }
        
        // Prepare replication request
        ReplicationRequest request = ReplicationRequest.builder()
            .requestId(generateRequestId())
            .entry(entry)
            .generationId(generationId)
            .build();
        
        // Submit replication tasks to followers in parallel
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (String followerUrl : followerUrls) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(
                () -> replicateToFollower(followerUrl, request),
                replicationExecutor
            );
            futures.add(future);
        }
        
        // Wait for responses with timeout
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            allFutures.get(replicationTimeoutMs, TimeUnit.MILLISECONDS);
            
            // Count successful replications
            for (CompletableFuture<Boolean> future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally() && future.get()) {
                    successfulWrites++;
                }
            }
            
        } catch (TimeoutException e) {
            log.warn("Replication timeout after {}ms", replicationTimeoutMs);
            // Count completed futures
            for (CompletableFuture<Boolean> future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    try {
                        if (future.get()) {
                            successfulWrites++;
                        }
                    } catch (Exception ex) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during replication", e);
        }
        
        // Record metrics
        Duration latency = Duration.between(start, Instant.now());
        replicationLatencyTimer.record(latency);
        
        boolean quorumAchieved = successfulWrites >= writeQuorum;
        if (quorumAchieved) {
            replicationSuccessCounter.increment();
        } else {
            replicationFailureCounter.increment();
        }
        
        log.info("Replication completed: {}/{} successful writes, quorum: {}", 
                 successfulWrites, replicationFactor, quorumAchieved);
        
        return quorumAchieved;
    }
    
    private boolean replicateToFollower(String followerUrl, ReplicationRequest request) {
        try {
            String url = followerUrl + "/api/storage/replicate";
            ReplicationResponse response = restTemplate.postForObject(
                url, request, ReplicationResponse.class
            );
            return response != null && response.isSuccess();
        } catch (Exception e) {
            log.error("Failed to replicate to follower: {}", followerUrl, e);
            return false;
        }
    }
    
    /**
     * Handle incoming replication request (as a follower)
     */
    public ReplicationResponse handleReplication(ReplicationRequest request) {
        try {
            repository.put(request.getEntry().getKey(), request.getEntry());
            
            return ReplicationResponse.builder()
                .requestId(request.getRequestId())
                .success(true)
                .nodeId(getNodeId())
                .version(request.getEntry().getVersion())
                .build();
        } catch (Exception e) {
            log.error("Failed to handle replication request", e);
            return ReplicationResponse.builder()
                .requestId(request.getRequestId())
                .success(false)
                .nodeId(getNodeId())
                .build();
        }
    }
    
    private String generateRequestId() {
        return System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(10000);
    }
    
    @Value("${storage.node-id}")
    private String nodeId;
    
    private String getNodeId() {
        return nodeId;
    }
}
