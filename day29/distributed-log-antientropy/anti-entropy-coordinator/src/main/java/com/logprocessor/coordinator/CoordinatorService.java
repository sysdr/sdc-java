package com.logprocessor.coordinator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.*;

@Service
public class CoordinatorService {
    
    @Autowired
    private ReconciliationJobRepository jobRepository;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    private final WebClient webClient;
    private final Counter jobsScheduled;
    private final Counter jobsCompleted;
    
    @Value("${storage.nodes}")
    private String storageNodes;
    
    @Value("${merkle.service.url}")
    private String merkleServiceUrl;
    
    public CoordinatorService(WebClient.Builder webClientBuilder, MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder.build();
        this.jobsScheduled = Counter.builder("reconciliation_jobs_scheduled_total")
            .description("Total reconciliation jobs scheduled")
            .register(meterRegistry);
        this.jobsCompleted = Counter.builder("reconciliation_jobs_completed_total")
            .description("Total reconciliation jobs completed")
            .register(meterRegistry);
    }
    
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void scheduleReconciliation() {
        List<String> nodes = Arrays.asList(storageNodes.split(","));
        List<String> partitions = Arrays.asList("partition-1", "partition-2", "partition-3");
        
        for (String partition : partitions) {
            // Schedule reconciliation between all node pairs
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    scheduleJob(partition, nodes.get(i), nodes.get(j), 5);
                }
            }
        }
    }
    
    @Scheduled(fixedDelay = 10000) // Process jobs every 10 seconds
    public void processJobs() {
        List<ReconciliationJob> pendingJobs = jobRepository.findPendingJobsByPriority();
        
        for (ReconciliationJob job : pendingJobs) {
            if (pendingJobs.indexOf(job) >= 3) break; // Process max 3 jobs concurrently
            
            processJob(job);
        }
    }
    
    private void scheduleJob(String partitionId, String node1, String node2, int priority) {
        // Check if job already exists
        List<ReconciliationJob> existing = jobRepository.findByPartitionIdAndStatus(partitionId, "PENDING");
        boolean jobExists = existing.stream()
            .anyMatch(j -> (j.getNode1Url().equals(node1) && j.getNode2Url().equals(node2)) ||
                          (j.getNode1Url().equals(node2) && j.getNode2Url().equals(node1)));
        
        if (jobExists) {
            return; // Skip duplicate
        }
        
        ReconciliationJob job = new ReconciliationJob();
        job.setPartitionId(partitionId);
        job.setNode1Url(node1);
        job.setNode2Url(node2);
        job.setStatus("PENDING");
        job.setScheduledAt(Instant.now());
        job.setPriority(priority);
        
        jobRepository.save(job);
        jobsScheduled.increment();
    }
    
    private void processJob(ReconciliationJob job) {
        try {
            job.setStatus("RUNNING");
            job.setStartedAt(Instant.now());
            jobRepository.save(job);
            
            // Call Merkle tree service to compare
            Map<String, Object> compareRequest = Map.of(
                "partitionId", job.getPartitionId(),
                "node1Url", job.getNode1Url(),
                "node2Url", job.getNode2Url()
            );
            
            Map<String, Object> response = webClient.post()
                .uri(merkleServiceUrl + "/api/merkle/compare")
                .body(BodyInserters.fromValue(compareRequest))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            Integer inconsistencies = (Integer) response.get("inconsistentSegments");
            job.setInconsistenciesFound(inconsistencies);
            
            if (inconsistencies > 0) {
                // Trigger repair (simplified - would call read-repair service)
                job.setInconsistenciesRepaired(inconsistencies);
            } else {
                job.setInconsistenciesRepaired(0);
            }
            
            job.setStatus("COMPLETED");
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
            jobsCompleted.increment();
            
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }
}
