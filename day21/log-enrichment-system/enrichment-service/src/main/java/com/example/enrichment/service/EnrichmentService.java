package com.example.enrichment.service;

import com.example.enrichment.model.EnrichedLogEvent;
import com.example.enrichment.model.EnrichmentMetadata;
import com.example.enrichment.model.LogEvent;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class EnrichmentService {
    
    private final MetadataResolver metadataResolver;
    private final MeterRegistry meterRegistry;
    private final Counter enrichmentAttempts;
    private final Counter enrichmentSuccesses;
    private final Counter enrichmentFailures;
    
    public EnrichmentService(MetadataResolver metadataResolver, MeterRegistry meterRegistry) {
        this.metadataResolver = metadataResolver;
        this.meterRegistry = meterRegistry;
        
        this.enrichmentAttempts = Counter.builder("enrichment.attempts")
            .description("Total enrichment attempts")
            .register(meterRegistry);
        
        this.enrichmentSuccesses = Counter.builder("enrichment.successes")
            .description("Successful enrichments")
            .register(meterRegistry);
        
        this.enrichmentFailures = Counter.builder("enrichment.failures")
            .description("Failed enrichments")
            .register(meterRegistry);
    }
    
    @Timed(value = "enrichment.latency", description = "Time taken to enrich a log event")
    public EnrichedLogEvent enrich(LogEvent logEvent) {
        enrichmentAttempts.increment();
        
        try {
            EnrichmentMetadata metadata = metadataResolver.resolve(logEvent);
            metadata.setEnrichedAt(Instant.now());
            
            trackCoverage(metadata);
            
            boolean isFullyEnriched = metadata.getFailedLookups().isEmpty();
            
            if (isFullyEnriched) {
                enrichmentSuccesses.increment();
                return EnrichedLogEvent.success(logEvent, metadata);
            } else {
                log.warn("Partial enrichment for log {}: failed lookups = {}", 
                    logEvent.getId(), metadata.getFailedLookups());
                return EnrichedLogEvent.partial(logEvent, metadata);
            }
            
        } catch (Exception e) {
            enrichmentFailures.increment();
            log.error("Enrichment failed for log {}: {}", logEvent.getId(), e.getMessage());
            return EnrichedLogEvent.failed(logEvent, e.getMessage());
        }
    }
    
    private void trackCoverage(EnrichmentMetadata metadata) {
        int totalFields = 6; // hostname, datacenter, environment, serviceVersion, deploymentId, costCenter
        int populatedFields = 0;
        
        if (metadata.getHostname() != null) populatedFields++;
        if (metadata.getDatacenter() != null) populatedFields++;
        if (metadata.getEnvironment() != null) populatedFields++;
        if (metadata.getServiceVersion() != null) populatedFields++;
        if (metadata.getDeploymentId() != null) populatedFields++;
        if (metadata.getCostCenter() != null) populatedFields++;
        
        double coverage = (double) populatedFields / totalFields;
        meterRegistry.gauge("enrichment.coverage", coverage);
    }
}
