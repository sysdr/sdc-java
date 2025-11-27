package com.example.enrichment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentMetadata {
    private String hostname;
    private String datacenter;
    private String environment;
    
    @JsonProperty("service_version")
    private String serviceVersion;
    
    @JsonProperty("deployment_id")
    private String deploymentId;
    
    @JsonProperty("cost_center")
    private String costCenter;
    
    @Builder.Default
    private Map<String, String> tags = new HashMap<>();
    
    @JsonProperty("enriched_at")
    private Instant enrichedAt;
    
    @JsonProperty("enrichment_version")
    @Builder.Default
    private String enrichmentVersion = "v1.0";
    
    @JsonProperty("enrichment_status")
    @Builder.Default
    private String enrichmentStatus = "complete";
    
    @JsonProperty("failed_lookups")
    @Builder.Default
    private Map<String, String> failedLookups = new HashMap<>();
}
