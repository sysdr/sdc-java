package com.example.enrichment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedLogEvent {
    @JsonProperty("original_event")
    private LogEvent originalEvent;
    
    private EnrichmentMetadata enrichment;
    
    @JsonProperty("is_fully_enriched")
    private boolean isFullyEnriched;
    
    public static EnrichedLogEvent success(LogEvent event, EnrichmentMetadata metadata) {
        metadata.setEnrichmentStatus("complete");
        return EnrichedLogEvent.builder()
            .originalEvent(event)
            .enrichment(metadata)
            .isFullyEnriched(metadata.getFailedLookups().isEmpty())
            .build();
    }
    
    public static EnrichedLogEvent partial(LogEvent event, EnrichmentMetadata metadata) {
        metadata.setEnrichmentStatus("partial");
        return EnrichedLogEvent.builder()
            .originalEvent(event)
            .enrichment(metadata)
            .isFullyEnriched(false)
            .build();
    }
    
    public static EnrichedLogEvent failed(LogEvent event, String error) {
        EnrichmentMetadata metadata = EnrichmentMetadata.builder()
            .enrichmentStatus("failed")
            .build();
        metadata.getFailedLookups().put("enrichment_error", error);
        
        return EnrichedLogEvent.builder()
            .originalEvent(event)
            .enrichment(metadata)
            .isFullyEnriched(false)
            .build();
    }
}
