package com.example.coordinator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartitionMetadata {
    private String partitionId;
    private Instant minTimestamp;
    private Instant maxTimestamp;
    private Long logCount;
    private Set<String> logLevels;
    private transient BloomFilter<String> serviceNameBloomFilter;
    private String nodeUrl;
    
    public boolean mightContainService(String serviceName) {
        if (serviceNameBloomFilter == null) {
            initializeBloomFilter();
        }
        return serviceNameBloomFilter.mightContain(serviceName);
    }
    
    public void addServiceToBloomFilter(String serviceName) {
        if (serviceNameBloomFilter == null) {
            initializeBloomFilter();
        }
        serviceNameBloomFilter.put(serviceName);
    }
    
    private void initializeBloomFilter() {
        serviceNameBloomFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            1000, // Expected insertions
            0.01  // False positive probability
        );
    }
}
