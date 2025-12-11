package com.example.partition.service;

import com.example.partition.repository.LogEntryRepository;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {
    
    private final LogEntryRepository repository;
    
    @Value("${partition.id:partition-1}")
    private String partitionId;
    
    private Instant minTimestamp;
    private Instant maxTimestamp;
    private Set<String> logLevels;
    private BloomFilter<String> serviceNameBloomFilter;
    
    @PostConstruct
    public void initialize() {
        refreshMetadata();
    }
    
    public void refreshMetadata() {
        log.info("Refreshing partition metadata for {}", partitionId);
        
        minTimestamp = repository.findMinTimestamp();
        maxTimestamp = repository.findMaxTimestamp();
        logLevels = new HashSet<>(repository.findDistinctLogLevels());
        
        // Build bloom filter for service names
        serviceNameBloomFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            1000,
            0.01
        );
        
        List<String> serviceNames = repository.findDistinctServiceNames();
        serviceNames.forEach(serviceNameBloomFilter::put);
        
        log.info("Metadata refreshed: {} services, {} log levels, range=[{} - {}]",
            serviceNames.size(), logLevels.size(), minTimestamp, maxTimestamp);
    }
    
    public Map<String, Object> getMetadata() {
        return Map.of(
            "partitionId", partitionId,
            "minTimestamp", minTimestamp != null ? minTimestamp : Instant.now(),
            "maxTimestamp", maxTimestamp != null ? maxTimestamp : Instant.now(),
            "logCount", repository.count(),
            "logLevels", logLevels != null ? logLevels : Collections.emptySet()
        );
    }
}
