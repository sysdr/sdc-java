package com.example.coordinator.service;

import com.example.coordinator.model.LogQuery;
import com.example.coordinator.model.PartitionMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PartitionMetadataCache {
    
    private final Map<String, PartitionMetadata> cache = new ConcurrentHashMap<>();
    private final WebClient.Builder webClientBuilder;
    
    @Value("${partition.nodes:http://partition-node-1:8081,http://partition-node-2:8082,http://partition-node-3:8083}")
    private List<String> partitionNodes;
    
    public PartitionMetadataCache(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    @PostConstruct
    public void initialize() {
        refreshMetadata();
    }
    
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void refreshMetadata() {
        log.info("Refreshing partition metadata from {} nodes", partitionNodes.size());
        
        Flux.fromIterable(partitionNodes)
            .flatMap(this::fetchPartitionMetadata)
            .doOnNext(metadata -> {
                cache.put(metadata.getPartitionId(), metadata);
                log.debug("Updated metadata for partition: {}", metadata.getPartitionId());
            })
            .doOnError(e -> log.error("Error refreshing metadata", e))
            .subscribe();
    }
    
    private Mono<PartitionMetadata> fetchPartitionMetadata(String nodeUrl) {
        return webClientBuilder.build()
            .get()
            .uri(nodeUrl + "/api/partition/metadata")
            .retrieve()
            .bodyToMono(PartitionMetadata.class)
            .timeout(Duration.ofSeconds(3))
            .doOnNext(metadata -> metadata.setNodeUrl(nodeUrl))
            .onErrorResume(e -> {
                log.warn("Failed to fetch metadata from {}: {}", nodeUrl, e.getMessage());
                return Mono.empty();
            });
    }
    
    public Set<PartitionMetadata> prunePartitions(LogQuery query) {
        return cache.values().stream()
            .filter(metadata -> matchesTimeRange(metadata, query))
            .filter(metadata -> matchesLogLevel(metadata, query))
            .filter(metadata -> matchesServiceName(metadata, query))
            .collect(Collectors.toSet());
    }
    
    private boolean matchesTimeRange(PartitionMetadata metadata, LogQuery query) {
        if (query.getStartTime() == null && query.getEndTime() == null) {
            return true;
        }
        
        Instant partitionMax = metadata.getMaxTimestamp();
        Instant partitionMin = metadata.getMinTimestamp();
        
        if (query.getStartTime() != null && partitionMax.isBefore(query.getStartTime())) {
            return false;
        }
        
        if (query.getEndTime() != null && partitionMin.isAfter(query.getEndTime())) {
            return false;
        }
        
        return true;
    }
    
    private boolean matchesLogLevel(PartitionMetadata metadata, LogQuery query) {
        if (query.getLogLevel() == null) {
            return true;
        }
        return metadata.getLogLevels().contains(query.getLogLevel());
    }
    
    private boolean matchesServiceName(PartitionMetadata metadata, LogQuery query) {
        if (query.getServiceName() == null) {
            return true;
        }
        return metadata.mightContainService(query.getServiceName());
    }
    
    public Collection<PartitionMetadata> getAllPartitions() {
        return cache.values();
    }
    
    public int getPartitionCount() {
        return cache.size();
    }
}
